module.exports = {
    default: {
        paths: ['src/test/resources/features/**/*.feature'],
        import: ['src/test/resources/steps/**/*.js'],
        // Scenarios run in a pseudo-random order; pass --order random:<seed> to change the seed
        order: 'random:20261117'
    }
};
