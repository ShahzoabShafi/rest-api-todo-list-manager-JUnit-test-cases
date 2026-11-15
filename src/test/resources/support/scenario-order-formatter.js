const { Formatter } = require('@cucumber/cucumber');

// Prints every scenario in the order it runs, with its result, then the totals.
// Used to show that the suite runs in a different order with each seed.
class ScenarioOrderFormatter extends Formatter {
    constructor(options) {
        super(options);
        const counts = { PASSED: 0, FAILED: 0 };
        let position = 0;

        options.eventBroadcaster.on('envelope', (envelope) => {
            if (envelope.testCaseFinished) {
                const attempt = this.eventDataCollector.getTestCaseAttempt(envelope.testCaseFinished.testCaseStartedId);
                const status = attempt.worstTestStepResult.status === 'PASSED' ? 'PASSED' : 'FAILED';
                const feature = attempt.pickle.uri.split('/').pop().replace('.feature', '');
                counts[status] += 1;
                position += 1;
                this.log(`${String(position).padStart(2)}. ${status}  ${feature}: ${attempt.pickle.name}\n`);
            }
            if (envelope.testRunFinished) {
                this.log(`\n${position} scenarios (${counts.PASSED} passed, ${counts.FAILED} failed)\n`);
            }
        });
    }
}

module.exports = ScenarioOrderFormatter;