import Store from '@dojo/framework/stores/Store';
import { State } from '../../interfaces';
import { StoreContainer } from '@dojo/framework/stores/StoreInjector';
import ListComponentRepo, { ListComponentRepoProperties } from '../../pages/marketplace/ListComponentRepo';

function getProperties(store: Store<State>): ListComponentRepoProperties {
	const { get, path } = store;

	return {
		loggedUsername: get(path('user', 'loginName')),
		componentRepos: get(path('componentRepos'))
	};
}

export default StoreContainer(ListComponentRepo, 'state', {
	paths: [['user'], ['componentRepos']],
	getProperties
});
