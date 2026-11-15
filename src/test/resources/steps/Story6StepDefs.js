const { When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

// --- When steps ---

When('a user creates a project with title {string}, description {string} and active {string}',
    async function (title, description, active) {
        this.projectCountBefore = await api.countOf('/projects', 'projects');
        const res = await api.http().post('/projects')
            .send({ title: title, description: description, active: api.toBooleanOrRaw(active) });
        api.record(this, res);
    });

When('a user creates a project with only the title {string}', async function (title) {
    this.projectCountBefore = await api.countOf('/projects', 'projects');
    const res = await api.http().post('/projects').send({ title: title });
    api.record(this, res);
});

// --- Then steps ---

// The API returns booleans as strings ("true"/"false"), so compare as strings
Then('a project titled {string} exists with description {string} and active {string}',
    async function (title, description, active) {
        const project = await api.findByTitle('/projects', 'projects', title);
        expect(project.description).to.equal(description);
        expect(String(project.active)).to.equal(active);
    });

Then('the project titled {string} is not completed', async function (title) {
    const project = await api.findByTitle('/projects', 'projects', title);
    expect(String(project.completed)).to.equal('false');
});

Then('the number of projects increases by {int}', async function (amount) {
    expect(await api.countOf('/projects', 'projects')).to.equal(this.projectCountBefore + amount);
});

Then('the number of projects is unchanged', async function () {
    expect(await api.countOf('/projects', 'projects')).to.equal(this.projectCountBefore);
});

Then('the user is notified of the completion of the creation operation', function () {
    expect(this.response).to.have.status(201);
});
