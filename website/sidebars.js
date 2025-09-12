/** @type {import('@docusaurus/plugin-content-docs').SidebarsConfig} */
const sidebars = {
  docs: [
    {
      type: 'category',
      label: 'Git Protocol',
      collapsed: false,
      items: [
        'git-protocol/intro',
        'git-protocol/1-discovering-references',
        'git-protocol/2-git-upload-service',
        'git-protocol/3-pack-header',
        'git-protocol/4-object-header',
        'git-protocol/5-parsing-ref-delta',
        'git-protocol/6-reading-pack-file',
      ],
    },
  ],
};

module.exports = sidebars;


