import java.io.File;
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.DataFormatException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;


public class Main {
    public static void main(String[] args) {
        System.out.println("Hello from GitLite!");
        if (args.length == 2 && "clone".equals(args[0])) {
            String repoUrl = args[1];
            try {
              cloneRepository(repoUrl);
            } catch (Exception e) {
              throw new RuntimeException(e);
            }
        } else {
          System.out.println("Unknown command: " + (args.length > 0 ? args[0] : ""));
        }
    }

    private static void cloneRepository(String repoUrl) throws Exception {
        String repoName = deriveRepoName(repoUrl);
        File dir = new File(System.getProperty("user.home") + File.separator + "Downloads"  + File.separator + repoName);
        dir.mkdirs();
    
         // Initialize .git directory structure
         File gitDir = new File(dir, ".git");
        new File(gitDir, "objects").mkdirs();
        new File(gitDir, "refs/heads").mkdirs();
        
        // Step 1: Discover refs using Smart HTTP protocol
        HttpClient client = HttpClient.newHttpClient();
        
        // Get refs from info/refs?service=git-upload-pack
        String refsUrl = repoUrl + "/info/refs?service=git-upload-pack";
        HttpRequest refsRequest = HttpRequest.newBuilder()
            .uri(URI.create(refsUrl))
            .GET()
            .build();
        
        HttpResponse<byte[]> refsResponse = client.send(refsRequest, HttpResponse.BodyHandlers.ofByteArray());
        Map<String, String> refs = parseRefs(refsResponse.body());
        
        // Find HEAD ref
        String headCommit = refs.get("HEAD");
        
        // If no HEAD, try to find the default branch
        if (headCommit == null) {
          // Try common default branch names
          for (String branch : Arrays.asList("refs/heads/master", "refs/heads/main")) {
            if (refs.containsKey(branch)) {
              headCommit = refs.get(branch);
              break;
            }
          }
        }
        
        if (headCommit == null) {
          // System.err.println("Available refs: " + refs);
          throw new RuntimeException("Could not determine HEAD commit");
        }
        
        // Step 2: Request packfile using Smart HTTP (pkt-line), with side-band-64k
        String packUrl = repoUrl + "/git-upload-pack";
        // Build want list for all refs (fetch full repo)
        // Ask for non-delta, side-band stream to keep parsing simple and fast
        String caps = "side-band-64k no-progress agent=codecrafters-git-java";
        ByteArrayOutputStream reqBody = new ByteArrayOutputStream();
        boolean first = true;
        for (Map.Entry<String, String> e : refs.entrySet()) {
          String refName = e.getKey();
          if (!refName.startsWith("refs/")) continue;
          String sha = e.getValue();
          String line = first ? ("want " + sha + " " + caps + "\n") : ("want " + sha + "\n");
          reqBody.write(pktLine(line));
          first = false;
        }
        // Also ensure we want headCommit if no refs matched
        if (first) {
          reqBody.write(pktLine("want " + headCommit + " " + caps + "\n"));
        }
        // Finalize negotiation: flush wants, then send done
        reqBody.write("0000".getBytes(StandardCharsets.US_ASCII));
        reqBody.write(pktLine("done\n"));
    
        HttpRequest packRequest = HttpRequest.newBuilder()
            .uri(URI.create(packUrl))
            .header("Content-Type", "application/x-git-upload-pack-request")
            .POST(HttpRequest.BodyPublishers.ofByteArray(reqBody.toByteArray()))
            .build();
    
        HttpResponse<byte[]> packResponse = client.send(packRequest, HttpResponse.BodyHandlers.ofByteArray());
    
        // Step 3: Demultiplex side-band and reconstruct raw PACK bytes
        byte[] packBytes = extractPackFromSideBand(packResponse.body());
    
        // Step 4: Parse PACK (supports delta objects) and write loose objects
        Map<String, byte[]> objects = parsePackObjects(packBytes);
        for (Map.Entry<String, byte[]> e : objects.entrySet()) {
          writeObjectToStore(e.getValue(), e.getKey(), gitDir);
        }
    
        // Step 5: Write refs
        for (Map.Entry<String, String> ref : refs.entrySet()) {
          if (ref.getKey().startsWith("refs/") && !ref.getKey().equals("HEAD")) {
            File refFile = new File(gitDir, ref.getKey());
            refFile.getParentFile().mkdirs();
            Files.write(refFile.toPath(), (ref.getValue() + "\n").getBytes());
          }
           // throw new RuntimeException(ex);
        
        }
        
        // Write HEAD
        File headFile = new File(gitDir, "HEAD");
        // For simplicity, write direct reference to commit
        Files.write(headFile.toPath(), (headCommit + "\n").getBytes());
        
        // Step 6: Automatically populate working tree (no external git)
        try {
          checkoutCommitFromObjects(dir, headCommit, objects);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
      }
    
      private static byte[] sha1(byte[] data) {
        try {
          MessageDigest md = MessageDigest.getInstance("SHA-1");
          return md.digest(data);
        } catch (NoSuchAlgorithmException e) {
          throw new RuntimeException(e);
        }
      }
    // stay
      private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
      }
      
      private static void writeObjectToStore(byte[] storeBytes, String shaHex, File gitDir) throws IOException {
        String dir = shaHex.substring(0, 2);
        String file = shaHex.substring(2);
        File objectsDir = new File(gitDir, "objects/" + dir);
        objectsDir.mkdirs();
        File objFile = new File(objectsDir, file);
        if (objFile.exists()) return;
        try (DeflaterOutputStream deflater = new DeflaterOutputStream(Files.newOutputStream(objFile.toPath()))) {
          deflater.write(storeBytes);
        }
      }

      private static String deriveRepoName(String repoUrl) {
        try {
          String path = URI.create(repoUrl).getPath();
          if (path == null || path.isEmpty()) return "repo";
          // Split and take the last non-empty segment
          String[] parts = path.split("/");
          String last = "";
          for (int i = parts.length - 1; i >= 0; i--) {
            if (parts[i] != null && !parts[i].isEmpty()) { last = parts[i]; break; }
          }
          if (last.isEmpty()) return "repo";
          if (last.endsWith(".git")) last = last.substring(0, last.length() - 4);
          return last;
        } catch (Exception e) {
          return "repo";
        }
      }
    
      private static byte[] pktLine(String s) throws IOException {
        byte[] payload = s.getBytes(StandardCharsets.UTF_8);
        String len = String.format("%04x", payload.length + 4);
        ByteArrayOutputStream out = new ByteArrayOutputStream(payload.length + 4);
        out.write(len.getBytes(StandardCharsets.US_ASCII));
        out.write(payload);
        return out.toByteArray();
      }
    
      private static byte[] extractPackFromSideBand(byte[] body) {
        int pos = 0;
        ByteArrayOutputStream pack = new ByteArrayOutputStream();
        while (pos + 4 <= body.length) {
          String lenStr = new String(body, pos, 4, StandardCharsets.US_ASCII);
          if (!lenStr.matches("[0-9a-fA-F]{4}")) break;
          int len = Integer.parseInt(lenStr, 16);
          pos += 4;
          if (len == 0) {
            // flush packet, continue to next pkt-line (don't terminate early)
            continue;
          }
          if (pos + len - 4 > body.length) break;
          int chan = body[pos] & 0xff;
          if (chan == 1) {
            pack.write(body, pos + 1, len - 5); // exclude channel byte
          }
          // ignore channels 2 (progress) and 3 (fatal)
          pos += len - 4;
        }
        return pack.toByteArray();
      }
    
      // PACK parser with delta support: commit(1), tree(2), blob(3), tag(4), ofs-delta(6), ref-delta(7)
      private static Map<String, byte[]> parsePackObjects(byte[] pack) {
        Map<String, byte[]> out = new HashMap<>();
        int hdr = 0;
        while (hdr + 4 <= pack.length && !(pack[hdr] == 'P' && pack[hdr+1] == 'A' && pack[hdr+2] == 'C' && pack[hdr+3] == 'K')) {
          hdr++;
        }
        if (hdr + 12 > pack.length) return out;
        int offStart = hdr + 4; // after 'PACK'
        offStart += 4; // skip version
        int num = ((pack[offStart] & 0xff) << 24) | ((pack[offStart+1] & 0xff) << 16) | ((pack[offStart+2] & 0xff) << 8) | (pack[offStart+3] & 0xff);
        offStart += 4;
    
        // Pass 1: store non-delta objects (types 1..4)
        int off = offStart;
        for (int i = 0; i < num && off < pack.length; i++) {
          int b = pack[off++] & 0xff;
          int type = (b >> 4) & 7;
          while ((b & 0x80) != 0) { b = pack[off++] & 0xff; }
          if (type == 6) { throw new RuntimeException("OFS_DELTA (type 6) not supported"); }
          if (type == 7) {
            if (off + 20 > pack.length) throw new RuntimeException("Truncated REF_DELTA base id");
            off += 20; // skip base id
            // skip delta payload
            inflateAt(pack, off);
            off += lastInflateConsumed;
            continue;
          }
          byte[] data = inflateAt(pack, off);
          off += lastInflateConsumed;
          String typeStr = switch (type) {
            case 1 -> "commit";
            case 2 -> "tree";
            case 3 -> "blob";
            case 4 -> "tag";
            default -> throw new RuntimeException("Unknown type: " + type);
          };
          byte[] header = (typeStr + " " + data.length + "\0").getBytes(StandardCharsets.UTF_8);
          byte[] full = new byte[header.length + data.length];
          System.arraycopy(header, 0, full, 0, header.length);
          System.arraycopy(data, 0, full, header.length, data.length);
          String sha = toHex(sha1(full));
          out.put(sha, full);
        }
    
        // Pass 2: resolve REF_DELTA (type 7) using stored bases
        off = offStart;
        for (int i = 0; i < num && off < pack.length; i++) {
          int b = pack[off++] & 0xff;
          int type = (b >> 4) & 7;
          while ((b & 0x80) != 0) { b = pack[off++] & 0xff; }
          if (type == 6) { throw new RuntimeException("OFS_DELTA (type 6) not supported"); }
          if (type != 7) {
            // skip non-delta payload
            inflateAt(pack, off);
            off += lastInflateConsumed;
            continue;
          }
          if (off + 20 > pack.length) throw new RuntimeException("Truncated REF_DELTA base id");
          byte[] baseId = Arrays.copyOfRange(pack, off, off + 20);
          String baseSha = toHex(baseId);
          off += 20;
          byte[] deltaData = inflateAt(pack, off);
          off += lastInflateConsumed;
          byte[] baseFull = out.get(baseSha);
          if (baseFull == null) throw new RuntimeException("Base not found for REF_DELTA: " + baseSha);
          int idx = 0; while (idx < baseFull.length && baseFull[idx] != 0) idx++; idx++;
          String baseHeader = new String(baseFull, 0, idx - 1, StandardCharsets.UTF_8);
          String baseType = baseHeader.split(" ")[0];
          byte[] baseData = Arrays.copyOfRange(baseFull, idx, baseFull.length);
          byte[] result = applyDelta(baseData, deltaData);
          byte[] header = (baseType + " " + result.length + "\0").getBytes(StandardCharsets.UTF_8);
          byte[] full = new byte[header.length + result.length];
          System.arraycopy(header, 0, full, 0, header.length);
          System.arraycopy(result, 0, full, header.length, result.length);
          String sha = toHex(sha1(full));
          out.put(sha, full);
        }
        return out;
      }
    
      private static int lastInflateConsumed = 0;
      private static byte[] inflateAt(byte[] pack, int start) {
        Inflater inflater = new Inflater();
        inflater.setInput(pack, start, pack.length - start);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        try {
          while (!inflater.finished()) {
            int r = inflater.inflate(buf);
            if (r > 0) {
              out.write(buf, 0, r);
            } else if (r == 0) {
              if (inflater.needsDictionary()) {
                throw new RuntimeException("Inflater needs dictionary");
              }
              if (inflater.needsInput()) {
                // We provided the whole remaining input; if still needs input, break to avoid loop
                break;
              }
            }
          }
        } catch (DataFormatException e) {
          throw new RuntimeException(e);
        }
        lastInflateConsumed = (pack.length - start) - inflater.getRemaining();
        inflater.end();
        return out.toByteArray();
      }
    
    
    
      private static byte[] applyDelta(byte[] base, byte[] delta) {
        int pos = 0;
        // Read base size (varint) and discard value
        int b;
        do { b = delta[pos++] & 0xff; } while ((b & 0x80) != 0);
        // Read result size (varint)
        long resultSize = 0; int shift = 0;
        do { b = delta[pos++] & 0xff; resultSize |= (long)(b & 0x7f) << shift; shift += 7; } while ((b & 0x80) != 0);
        ByteArrayOutputStream out = new ByteArrayOutputStream((int) resultSize);
        while (pos < delta.length) {
          int opcode = delta[pos++] & 0xff;
          if ((opcode & 0x80) != 0) {
            // copy from base
            int cpOff = 0; int cpSize = 0;
            if ((opcode & 0x01) != 0) cpOff |= (delta[pos++] & 0xff);
            if ((opcode & 0x02) != 0) cpOff |= (delta[pos++] & 0xff) << 8;
            if ((opcode & 0x04) != 0) cpOff |= (delta[pos++] & 0xff) << 16;
            if ((opcode & 0x08) != 0) cpOff |= (delta[pos++] & 0xff) << 24;
            if ((opcode & 0x10) != 0) cpSize |= (delta[pos++] & 0xff);
            if ((opcode & 0x20) != 0) cpSize |= (delta[pos++] & 0xff) << 8;
            if ((opcode & 0x40) != 0) cpSize |= (delta[pos++] & 0xff) << 16;
            if (cpSize == 0) cpSize = 0x10000;
            out.write(base, cpOff, cpSize);
          } else if (opcode != 0) {
            // insert literal
            int len = opcode & 0x7f;
            out.write(delta, pos, len);
            pos += len;
          } else {
            throw new RuntimeException("Invalid delta opcode 0");
          }
        }
        return out.toByteArray();
      }
    
    
    
      private static void checkoutCommitFromObjects(File workTree, String commitSha, Map<String, byte[]> objects) throws IOException {
        byte[] commit = objects.get(commitSha);
        if (commit == null) throw new IOException("Commit not found in objects map");
        int idx0 = 0; while (idx0 < commit.length && commit[idx0] != 0) idx0++; idx0++;
        if (idx0 >= commit.length) throw new IOException("Invalid commit object: missing header terminator");
        String s = new String(commit, idx0, commit.length - idx0, StandardCharsets.UTF_8);
        String treeSha = null;
        for (String line : s.split("\n")) { if (line.startsWith("tree ")) { treeSha = line.substring(5).trim(); break; } }
        if (treeSha == null) throw new IOException("Tree not found in commit");
        checkoutTreeFromObjects(workTree, treeSha, objects);
      }
    
      private static void checkoutTreeFromObjects(File dir, String treeSha, Map<String, byte[]> objects) throws IOException {
        byte[] tree = objects.get(treeSha);
        if (tree == null) throw new IOException("Tree not found: " + treeSha);
        // skip header
        int idx = 0; while (idx < tree.length && tree[idx] != 0) idx++; idx++;
        while (idx < tree.length) {
          int modeStart = idx; while (idx < tree.length && tree[idx] != ' ') idx++; String mode = new String(tree, modeStart, idx - modeStart, StandardCharsets.UTF_8); idx++;
          int nameStart = idx; while (idx < tree.length && tree[idx] != 0) idx++; String name = new String(tree, nameStart, idx - nameStart, StandardCharsets.UTF_8); idx++;
          byte[] sha20 = Arrays.copyOfRange(tree, idx, idx + 20); idx += 20; String shaHex = toHex(sha20);
          File target = new File(dir, name);
          if (mode.equals("40000")) {
            target.mkdirs();
            checkoutTreeFromObjects(target, shaHex, objects);
          } else {
            byte[] blob = objects.get(shaHex);
            if (blob == null) throw new IOException("Blob not found: " + shaHex);
            int c = 0; while (c < blob.length && blob[c] != 0) c++; c++;
            byte[] content = Arrays.copyOfRange(blob, c, blob.length);
            target.getParentFile().mkdirs();
            Files.write(target.toPath(), content);
          }
        }
      }
     // stay
      // archive fallback removed per requirements
      
      private static Map<String, String> parseRefs(byte[] data) {
        Map<String, String> refs = new HashMap<>();
        String response = new String(data, StandardCharsets.UTF_8);
        
        // Debug print to see what we're getting
        // System.err.println("Refs response: " + response.substring(0, Math.min(200, response.length())));
        
        // Parse pkt-line format
        int pos = 0;
        while (pos < response.length()) {
          // Check if we have at least 4 chars for length
          if (pos + 4 > response.length()) break;
          
          String lenStr = response.substring(pos, pos + 4);
          if (!lenStr.matches("[0-9a-f]{4}")) break;
          
          int len = Integer.parseInt(lenStr, 16);
          if (len == 0) {
            pos += 4;
            continue; // flush packet
          }
          
          // Get the line content (subtract 4 for length prefix)
          if (pos + len > response.length()) break;
          String line = response.substring(pos + 4, pos + len);
          pos += len;
          
          // Remove trailing newline if present
          if (line.endsWith("\n")) {
            line = line.substring(0, line.length() - 1);
          }
          
          // Skip service announcement
          if (line.startsWith("# service=")) continue;
          
          // Parse ref line
          // Format can be: "<sha> <ref>" or "<sha> <ref>\0<capabilities>"
          int spaceIdx = line.indexOf(' ');
          if (spaceIdx > 0) {
            String sha = line.substring(0, spaceIdx);
            String rest = line.substring(spaceIdx + 1);
            
            // Check for null byte (capabilities)
            int nullIdx = rest.indexOf('\0');
            String refName = nullIdx >= 0 ? rest.substring(0, nullIdx) : rest;
            
            refs.put(refName, sha);
          }
        }
        
        return refs;
      }
}


