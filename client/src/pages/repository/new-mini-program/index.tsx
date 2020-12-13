import { create, tsx } from '@dojo/framework/core/vdom';
import i18n from '@dojo/framework/core/middleware/i18n';
import store from '../../../store';
import bundle from './nls/NewMiniProgram';
import Exception from '../../error/Exception';
import * as c from '@blocklang/bootstrap-classes';
import * as css from './new-mini-program.m.css';
import {
	initForNewGroupProcess,
	groupKeyInputProcess,
	groupNameInputProcess,
	saveGroupProcess,
} from '../../../processes/repositoryGroupProcesses';
import Spinner from '../../../widgets/spinner';
import { canNewGroup } from '../../../permission';
import Link from '@dojo/framework/routing/Link';
import { ValidateStatus, PageAppType, ResourceType } from '../../../constant';
import RepositoryHeader from '../../widgets/RepositoryHeader';

export interface NewMiniProgramProperties {
	owner: string;
	repoName: string;
}

const factory = create({ store, i18n }).properties<NewMiniProgramProperties>();

export default factory(function NewMiniProgram({ properties, middleware: { store, i18n } }) {
	const { messages } = i18n.localize(bundle);
	const { owner, repoName } = properties();
	const { get, path, executor } = store;

	const { loginName } = get(path('user'));
	const isLogin = !!loginName;
	if (!isLogin) {
		return <Exception type="403" />;
	}

	const repositoryResource = get(path('repositoryResource'));
	const isLoading = get(path('repositoryResource', 'isLoading'));
	const isLoaded = get(path('repositoryResource', 'isLoaded'));
	const saveFailedErrors = get(path('errors'));

	if (!repositoryResource && !isLoading) {
		executor(initForNewGroupProcess)({
			owner,
			repo: repoName,
		});
	}
	if (!isLoaded) {
		return <Spinner />;
	}

	// FIXME: 为什么上面从 properties 中获取数据，这里又从 store 中获取数据呢？
	const repo = get(path('repository'));
	const parentGroups = get(path('parentGroups')) || [];

	if (!canNewGroup(repo.accessLevel)) {
		return <Exception type="403" />;
	}

	function renderBreadcrumb() {
		return (
			<nav aria-label="breadcrumb">
				<ol classes={[c.breadcrumb, css.navOl]}>
					<li classes={[c.breadcrumb_item]}>
						<Link
							to="view-repo"
							params={{ owner: repo.createUserName, repo: repo.name }}
						>{`${repo.name}`}</Link>
						{parentGroups.map((item) => (
							<li>
								<Link
									to="view-repo-group"
									params={{ owner: repo.createUserName, repo: repo.name }}
									parentPath={item.path.substring(1)}
								>{`${item.name}`}</Link>
							</li>
						))}
					</li>
				</ol>
			</nav>
		);
	}

	function renderKeyInput() {
		const keyValidateStatus = get(path('groupInputValidation', 'keyValidateStatus')) || ValidateStatus.UNVALIDATED;
		const keyErrorMessage = get(path('groupInputValidation', 'keyErrorMessage'));
		const inputClasses = [c.form_control];
		if (keyValidateStatus === ValidateStatus.INVALID) {
			inputClasses.push(c.is_invalid);
		}

		return (
			<div classes={[c.mb_3]}>
				<label for="key">
					{messages.projectKeyLabel}
					<small classes={c.text_muted}>{`${messages.requiredLabel}`}</small>
				</label>
				<div classes={[c.input_group]}>
					<input
						type="text"
						id="key"
						classes={inputClasses}
						required={true}
						focus={true}
						maxlength={32}
						oninput={(event: KeyboardEvent<HTMLInputElement>) => {
							executor(groupKeyInputProcess)({
								key: event.target.value,
								owner: repo.createUserName,
								repo: repo.name,
								parentId: repositoryResource.id,
							});
						}}
					/>
					{keyValidateStatus === ValidateStatus.INVALID && (
						<div classes={[c.invalid_tooltip]} innerHTML={`${keyErrorMessage}`}></div>
					)}
				</div>
				<small classes={[c.form_text, c.text_muted]}>{`${messages.projectKeyHelp}`}</small>
			</div>
		);
	}

	function renderNameInput() {
		const nameValidateStatus =
			get(path('groupInputValidation', 'nameValidateStatus')) || ValidateStatus.UNVALIDATED;
		const nameErrorMessage = get(path('groupInputValidation', 'nameErrorMessage'));
		const inputClasses = [c.form_control];
		if (nameValidateStatus === ValidateStatus.INVALID) {
			inputClasses.push(c.is_invalid);
		}

		return (
			<div classes={[c.mb_3]}>
				<label for="name">{messages.projectNameLabel}</label>
				<div classes={[c.input_group]}>
					<input
						type="text"
						id="name"
						classes={inputClasses}
						maxlength={32}
						oninput={(event: KeyboardEvent<HTMLInputElement>) => {
							executor(groupNameInputProcess)({
								name: event.target.value,
								owner: repo.createUserName,
								repo: repo.name,
								parentId: repositoryResource.id,
							});
						}}
					/>
					{nameValidateStatus === ValidateStatus.INVALID && (
						<div classes={[c.invalid_tooltip]} innerHTML={`${nameErrorMessage}`}></div>
					)}
				</div>
				<small classes={[c.form_text, c.text_muted]}>{`${messages.projectNameHelp}`}</small>
			</div>
		);
	}

	function renderButtons() {
		const keyValidateStatus = get(path('groupInputValidation', 'keyValidateStatus')) || ValidateStatus.UNVALIDATED;
		const nameValidateStatus =
			get(path('groupInputValidation', 'nameValidateStatus')) || ValidateStatus.UNVALIDATED;
		// name 默认可以为空，所以可以不用填写，即不走校验。
		const disabled =
			keyValidateStatus === ValidateStatus.VALID && nameValidateStatus !== ValidateStatus.INVALID ? false : true;

		return (
			<div>
				<button
					type="button"
					classes={[c.btn, c.btn_primary]}
					disabled={disabled}
					onclick={
						disabled
							? undefined
							: (event: MouseEvent<HTMLButtonElement>) => {
									executor(saveGroupProcess)({
										owner: repo.createUserName,
										repo: repo.name,
										appType: PageAppType.MiniProgram,
										resourceType: ResourceType.Project,
									});
							  }
					}
				>{`${messages.projectSaveLabel}`}</button>{' '}
				<Link
					classes={[c.btn, c.btn_secondary]}
					to="view-repo"
					params={{
						owner: repo.createUserName,
						repo: repo.name,
					}}
				>{`${messages.projectCancelSaveLabel}`}</Link>
			</div>
		);
	}

	return (
		<div classes={[c.container]}>
			<RepositoryHeader repository={repo} privateRepositoryTitle={messages.privateRepositoryTitle} />
			<div classes={[c.container]} styles={{ maxWidth: '700px' }}>
				<h4>{messages.newMiniProgram}</h4>
				{renderBreadcrumb()}
				<hr />
				{saveFailedErrors && (
					<div classes={[c.alert, c.alert_danger, c.alert_dismissible]} role="alert">
						<h4 classes={[c.alert_heading]}>保存失败！</h4>
						<p innerHTML={saveFailedErrors.globalErrors[0]}></p>
					</div>
				)}
				<form classes={[c.needs_validation]} novalidate="novalidate">
					{renderKeyInput()}
					{renderNameInput()}
					<hr />
					{renderButtons()}
				</form>
			</div>
		</div>
	);
});
