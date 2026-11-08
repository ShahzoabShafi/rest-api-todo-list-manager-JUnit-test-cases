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

async function clearAll() {
    const collections = [['/todos', 'todos'], ['/categories', 'categories'], ['/projects', 'projects']];
    for (const [path, key] of collections) {
        for (const item of await list(path, key)) {
            await http().delete(`${path}/${item.id}`);
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

module.exports = {
    expect, http, list, clearAll, create,
    tryFindByTitle, findByTitle, errorText, record,
    associateCategory, linkTask
};
