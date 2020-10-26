module.exports = {
  env: {
    browser: true,
    es2021: true,
  },
  extends: [
    'plugin:react/recommended',
    'airbnb-typescript',
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaFeatures: {
      jsx: true,
    },
    ecmaVersion: 12,
    sourceType: 'module',
    project: './tsconfig.json',
  },
  plugins: [
    'react',
    '@typescript-eslint',
  ],
  globals: {
    React: 'writable',
    next: 'writable',
  },
  rules: {
    'react/react-in-jsx-scope': 'off',
    'require-jsdoc': 'off',
    'max-len': ['error', { code: 120, tabWidth: 2, comments: 120 }],
    'react/jsx-filename-extension': [1, { extensions: ['.tsx', '.ts'] }],
    'react/jsx-props-no-spreading': 'off',
    'comma-dangle': ['error', 'always-multiline'],
    'import/no-extraneous-dependencies': 'off',
    // 'react/jsx-max-props-per-line': ['error', { maximum: 2 }],
  },
};
