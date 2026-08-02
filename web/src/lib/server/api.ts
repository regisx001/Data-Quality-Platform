/**
 * @deprecated Import from "$lib/server/api/auth" or "$lib/server/api/datasource" instead.
 * This barrel file re-exports everything for backward compatibility.
 */

export { BACKEND_API_URL } from "./api/client";
export type { ApiError, ApiResult } from "./api/client";

export {
	registerUser,
	loginUser,
	verifyToken,
	getCurrentUser,
	updateUserProfile,
} from "./api/auth";
export type { UserProfile, AuthResponse } from "./api/auth";

export {
	getDatasources,
	getDatasourceById,
	createDatasource,
	updateDatasource,
	deleteDatasource,
	changeDatasourceStatus,
	getConfigSchemas,
	saveDatasourceConfig,
	getDatasourceConfig,
	testDatasourceConnection,
	discoverDatasourceDatasets,
	importDatasourceDatasets,
	uploadCsvFile,
} from "./api/datasource";

export {
	getDatasetById as getDatasetDetail,
	getDatasetPreview,
	profileDataset,
	deleteDataset as deleteDatasetEntity,
} from "./api/dataset";

export type {
	Datasource,
	DatasourceStatus,
	Dataset,
	ConfigField,
	ConnectorConfigSchema,
	ConnectionTestResult,
	DatasetDescriptor,
} from "./api/datasource";
