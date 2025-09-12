// @ts-check

/** @type {import('@docusaurus/types').Config} */
const config = {
  title: 'GitLite',
  tagline: 'A minimal Git client implemented in Java',
  url: 'https://kpraveenkumar19.github.io',
  baseUrl: '/gitlite/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'kpraveenkumar19',
  projectName: 'gitlite',
  deploymentBranch: 'gh-pages',
  i18n: {
    defaultLocale: 'en',
    locales: ['en'],
  },
  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */ ({
        docs: {
          path: 'docs',
          routeBasePath: '/docs',
          sidebarPath: require.resolve('./sidebars.js'),
          editUrl: undefined,
        },
        blog: false,
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],
  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */ ({
      navbar: {
        title: 'GitLite Docs',
        items: [
          { to: '/docs/git-protocol/intro', label: 'Docs', position: 'left' },
          { href: 'https://github.com/kpraveenkumar19/gitlite', label: 'GitHub', position: 'right' },
        ],
      },
      footer: {
        style: 'dark',
        links: [
          {
            title: 'Docs',
            items: [
              { label: 'Git Protocol', to: '/docs/git-protocol/intro' },
            ],
          },
          {
            title: 'Community',
            items: [
              { label: 'GitHub Issues', href: 'https://github.com/kpraveenkumar19/gitlite/issues' },
            ],
          },
        ],
        copyright: `Copyright © ${new Date().getFullYear()} GitLite`,
      },
      prism: {
        theme: require('prism-react-renderer/themes/github'),
        darkTheme: require('prism-react-renderer/themes/dracula'),
      },
    }),
};

module.exports = config;


