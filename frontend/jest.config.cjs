module.exports = {
  testEnvironment: 'node',
  rootDir: '.',
  testMatch: ['**/__tests__/**/*.test.js', '**/*.test.js'],
  collectCoverage: true,
  collectCoverageFrom: [
    'components/bottom-nav/bottom-nav.js',
    'components/loading-spinner/loading-spinner.js',
    'components/navigation-bar/navigation-bar.js',
    'components/restaurant-card/restaurant-card.js',
    'pages/spin/spin-logic.js',
    'api/client.js'
  ],
  coverageDirectory: 'coverage',
  coveragePathIgnorePatterns: ['/node_modules/']
};