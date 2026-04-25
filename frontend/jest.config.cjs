module.exports = {
  testEnvironment: 'node',
  rootDir: '.',
  testMatch: ['**/tests/**/*.test.js'],
  collectCoverageFrom: [
    'api/client.js',
    'components/bottom-nav/bottom-nav.js',
    'components/loading-spinner/loading-spinner.js',
    'components/navigation-bar/navigation-bar.js',
    'components/restaurant-card/restaurant-card.js',
    'pages/spin/spin-logic.js',
    'utils/ai-chat-session.js',
    'utils/rating-stars.js',
    'utils/restaurant-filters.js',
    'utils/restaurant-state.js'
  ],
  coverageDirectory: 'coverage',
  coveragePathIgnorePatterns: ['/node_modules/']
};