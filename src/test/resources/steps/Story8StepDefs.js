const { Given, When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

const findProject = api.findProject;

// POST amends only the fields sent (PUT would wipe the others: Part A bug 11)
async function amendProject(world, id, body) {
    const res = await api.http().post(`/projects/${id}`).send(body);
    api.record(world, res);
}

// The three "unchanged" steps differ only in which fields they compare
async function expectFieldsUnchanged(world, title, fields) {
    const current = await findProject(title);
    fields.forEach(field => expect(current[field], field).to.equal(world.originalProject[field]));
}

// --- Given steps ---

Given('a project with the title {string} exists', async function (title) {
    this.originalProject = await findProject(title);
});

Given('no project with the id {string} exists', async function (id) {
    const res = await api.http().get(`/projects/${id}`);
    expect(res).to.have.status(404);
});

// --- When steps ---

When('a user updates the description of the project titled {string} to {string}',
    async function (title, description) {
        const project = await findProject(title);
        await amendProject(this, project.id, { description: description });
    });

When('a user updates the completed status of the project titled {string} to {string}',
    async function (title, value) {
        const project = await findProject(title);
        await amendProject(this, project.id, { completed: api.toBooleanOrRaw(value) });
    });

When('a user updates the description of the project with id {string} to {string}',
    async function (id, description) {
        this.projectCountBefore = await api.countOf('/projects', 'projects');
        await amendProject(this, id, { description: description });
    });

// --- Then steps ---

Then('the project titled {string} has description {string}', async function (title, description) {
    expect((await findProject(title)).description).to.equal(description);
});

// The API returns booleans as strings ("true"/"false"), so compare as strings
Then('the project titled {string} has completed {string}', async function (title, completed) {
    expect(String((await findProject(title)).completed)).to.equal(completed);
});

Then('the title, active and completed status of the project titled {string} are unchanged',
    async function (title) {
        await expectFieldsUnchanged(this, title, ['title', 'active', 'completed']);
    });

Then('the title, description and active status of the project titled {string} are unchanged',
    async function (title) {
        await expectFieldsUnchanged(this, title, ['title', 'description', 'active']);
    });

Then('the project titled {string} keeps all of its original details', async function (title) {
    await expectFieldsUnchanged(this, title, ['title', 'description', 'active', 'completed']);
});
