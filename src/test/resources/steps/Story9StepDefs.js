const { When, Then } = require('@cucumber/cucumber');
const api = require('../support/api.js');

const expect = api.expect;

function findProject(title) {
    return api.findByTitle('/projects', 'projects', title);
}

async function taskIdsOf(projectId) {
    return (await api.list(`/projects/${projectId}/tasks`, 'todos')).map(t => t.id);
}

// Every "add" step posts to the project's tasks relationship; only the target and body differ
async function addTask(world, projectId, body) {
    const res = await api.http().post(`/projects/${projectId}/tasks`).send(body);
    api.record(world, res);
}

// --- When steps ---

When('a user adds the TODO titled {string} to the project {string}', async function (todoTitle, projectTitle) {
    const todo = await api.findByTitle('/todos', 'todos', todoTitle);
    const project = await findProject(projectTitle);
    await addTask(this, project.id, { id: todo.id });
});

// Undocumented: posting a title instead of an id creates a new todo already linked (Part A note 41)
When('a user adds a new task titled {string} with description {string} to the project {string}',
    async function (todoTitle, description, projectTitle) {
        this.todoCountBefore = (await api.list('/todos', 'todos')).length;   // Yessine's count steps read this too
        const project = await findProject(projectTitle);
        await addTask(this, project.id, { title: todoTitle, description: description });
    });

When('a user adds the TODO with id {string} to the project {string}', async function (todoId, projectTitle) {
    const project = await findProject(projectTitle);
    this.taskIdsBefore = await taskIdsOf(project.id);
    await addTask(this, project.id, { id: todoId });
});

When('a user adds the TODO titled {string} to the project with id {string}', async function (todoTitle, projectId) {
    const todo = await api.findByTitle('/todos', 'todos', todoTitle);
    await addTask(this, projectId, { id: todo.id });
});

// --- Then steps ---

Then('the project {string} lists the TODO titled {string} as a task', async function (projectTitle, todoTitle) {
    expect(await api.taskTitlesOf(projectTitle)).to.include(todoTitle);
});

Then('the TODO titled {string} lists the project {string}', async function (todoTitle, projectTitle) {
    expect(await api.projectTitlesOf(todoTitle)).to.include(projectTitle);
});

Then('the total number of todos increases by {int}', async function (amount) {
    expect((await api.list('/todos', 'todos')).length).to.equal(this.todoCountBefore + amount);
});

Then('the tasks of the project {string} are unchanged', async function (projectTitle) {
    const project = await findProject(projectTitle);
    expect(await taskIdsOf(project.id)).to.have.members(this.taskIdsBefore);
});
