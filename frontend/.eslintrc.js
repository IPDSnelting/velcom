module.exports = {
  root: true,

  env: {
    node: true
  },

  rules: {
    'no-debugger': process.env.NODE_ENV === 'production' ? 'warn' : 'off',
    'space-before-function-paren': 0,
    'no-new': 0,
    '@typescript-eslint/no-explicit-any': 0,
    '@typescript-eslint/no-non-null-assertion': 0,
    '@typescript-eslint/no-use-before-define': 0,
    '@typescript-eslint/interface-name-prefix': 0,
    '@typescript-eslint/camelcase': 0,
    'no-console': 'warn',
    '@typescript-eslint/no-inferrable-types': 0,
    'dot-notation': 0,
    'no-use-before-define': 0
  },

  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 2021
  },

  extends: [
    'plugin:vue/essential',
    '@vue/standard',
    '@vue/typescript',
    'eslint:recommended',
    '@vue/typescript/recommended',
    '@vue/prettier',
    '@vue/prettier/@typescript-eslint'
  ]
}
