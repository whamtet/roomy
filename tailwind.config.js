const colors = require('tailwindcss/colors')

module.exports = {
  content: [
      "./src/**/*.clj",
      "./resources/templates/*.{html,js}",
  ],
  theme: {
    extend: {},
    colors: {
      ...colors,
      'clj-blue': '#6180D2',
      'kkr-secondary': '#102434',
      'kkr-tertiary': '#1E3140',
      'kkr-purple': 'rgb(73, 0, 75)',
      'kkr-yellow': '#F9D953',
    }
  },
  plugins: [],
}
