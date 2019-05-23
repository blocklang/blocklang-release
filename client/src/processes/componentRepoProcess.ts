import { createProcess } from '@dojo/framework/stores/process';
import { commandFactory, getHeaders } from './utils';
import { baseUrl } from '../config';
import { replace } from '@dojo/framework/stores/state/operations';

const getComponentReposCommand = commandFactory(async ({ path, payload: { query = '', page = 1 } }) => {
	const response = await fetch(`${baseUrl}/component-repos?q=${query}&page=${page}`, {
		headers: getHeaders()
	});
	const json = await response.json();
	if (!response.ok) {
		return [replace(path('componentRepos'), undefined)];
	}

	return [replace(path('componentRepos'), json)];
});

export const initForListComponentReposProcess = createProcess('init-for-list-component-repo', [
	getComponentReposCommand
]);
