const { Given, When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

async function countOf(path, key) {
    return (await api.list(path, key)).length;
}

function findProject(title) {
    return api.findByTitle('/projects', 'projects', title);
}

// Counts are taken right before the DELETE, so "unchanged" / "decreases by" compare against the right moment
async function deleteProject(world, id) {
    world.projectCountBefore = await countOf('/projects', 'projects');
    world.todoCountBefore = await countOf('/todos', 'todos');   // Yessine's "total number of todos" steps read this
    const res = await api.http().delete(`/projects/${id}`);
    api.record(world, res);
}

// --- Given steps ---

Given('the project {string} has no tasks', async function (title) {
    const project = await findProject(title);
    expect(await api.list(`/projects/${project.id}/tasks`, 'todos')).to.have.lengthOf(0);
});

// Yessine's non-existence error step replaces <deleted id> with this.notedId
Given('the user has already deleted the project titled {string}', async function (title) {
    const project = await findProject(title);
    this.notedId = project.id;
    const res = await api.http().delete(`/projects/${project.id}`);
    expect(res).to.have.status(200);
});

// --- When steps ---

When('a user deletes the project titled {string}', async function (title) {
    const project = await findProject(title);
    this.notedId = project.id;
    await deleteProject(this, project.id);
});

When('a user deletes the same project again', async function () {
    await deleteProject(this, this.notedId);
});

When('a user deletes the project with id {string}', async function (id) {
    await deleteProject(this, id);
});

// --- Then steps ---

Then('the project titled {string} no longer exists', async function (title) {
    expect(await api.tryFindByTitle('/projects', 'projects', title)).to.equal(undefined);
    expect(await api.http().get(`/projects/${this.notedId}`)).to.have.status(404);
});

Then('the number of projects decreases by {int}', async function (amount) {
    expect(await countOf('/projects', 'projects')).to.equal(this.projectCountBefore - amount);
});
