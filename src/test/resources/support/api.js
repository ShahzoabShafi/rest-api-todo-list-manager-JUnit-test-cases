const chai = require('chai');
const chaiHttp = require('chai-http');

chai.use(chaiHttp);

chai.config.truncateThreshold = 0;

const expect = chai.expect;
const host = 'http://127.0.0.1:4567';
const http = () => chai.request(host);

async function list(path, key) {
    const res = await http().get(path);
    return res.body[key] || [];
}

const collections = [['/todos', 'todos'], ['/categories', 'categories'], ['/projects', 'projects']];

async function countOf(path, key) {
    return (await list(path, key)).length;
}

// The ids that exist right now, per collection, e.g. { '/todos': Set{'1','2'}, ... }
async function snapshotIds() {
    const snapshot = {};
    for (const [path, key] of collections) {
        snapshot[path] = new Set((await list(path, key)).map(item => item.id));
    }
    return snapshot;
}

// Delete everything created since the snapshot, so the server's default data is kept
async function restore(snapshot) {
    for (const [path, key] of collections) {
        for (const item of await list(path, key)) {
            if (!snapshot[path].has(item.id)) {
                await http().delete(`${path}/${item.id}`);
            }
        }
    }
}

async function create(path, body) {
    const res = await http().post(path).send(body);
    expect(res, `creating ${JSON.stringify(body)} at ${path}`).to.have.status(201);
    return res.body;
}

async function tryFindByTitle(path, key, title) {
    return (await list(path, key)).find(item => item.title === title);
}

async function findByTitle(path, key, title) {
    const item = await tryFindByTitle(path, key, title);
    expect(item, `"${title}" should exist at ${path}`).to.not.equal(undefined);
    return item;
}

function findProject(title) {
    return findByTitle('/projects', 'projects', title);
}

function errorText(res) {
    if (res.body && res.body.errorMessages) {
        return res.body.errorMessages.join(' ');
    }
    return res.text || '';
}

// Remember the last response and, if it was an error, the message the user would see
function record(world, res) {
    world.response = res;
    world.notification = res.status >= 400 ? errorText(res) : undefined;
}

// "true"/"false" become JSON booleans; any other text is sent as-is so invalid values reach the API
function toBooleanOrRaw(text) {
    if (text === 'true') return true;
    if (text === 'false') return false;
    return text;
}

async function associateCategory(todoTitle, categoryTitle) {
    const todo = await findByTitle('/todos', 'todos', todoTitle);
    const category = await findByTitle('/categories', 'categories', categoryTitle);
    const res = await http().post(`/todos/${todo.id}/categories`).send({ id: category.id });
    expect(res).to.have.status(201);
}

async function linkTask(projectTitle, todoTitle) {
    const todo = await findByTitle('/todos', 'todos', todoTitle);
    const project = await findByTitle('/projects', 'projects', projectTitle);
    const res = await http().post(`/projects/${project.id}/tasks`).send({ id: todo.id });
    expect(res).to.have.status(201);
}

// Titles of the todos a project lists as its tasks
async function taskTitlesOf(projectTitle) {
    const project = await findProject(projectTitle);
    return (await list(`/projects/${project.id}/tasks`, 'todos')).map(t => t.title);
}

// Titles of the projects a todo says it is a task of
async function projectTitlesOf(todoTitle) {
    const todo = await findByTitle('/todos', 'todos', todoTitle);
    return (await list(`/todos/${todo.id}/tasksof`, 'projects')).map(p => p.title);
}

module.exports = {
    expect, http, list, countOf, snapshotIds, restore, create,
    tryFindByTitle, findByTitle, findProject, errorText, record, toBooleanOrRaw,
    associateCategory, linkTask, taskTitlesOf, projectTitlesOf
};
