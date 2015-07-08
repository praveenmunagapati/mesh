package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;

public interface RoleClientMethods {

	Future<RoleResponse> findRoleByUuid(String uuid);

	Future<RoleListResponse> findRoles(PagingInfo pagingInfo);

	Future<RoleResponse> createRole(RoleCreateRequest roleCreateRequest);

	Future<GenericMessageResponse> deleteRole(String uuid);

	Future<RoleListResponse> findRolesForGroup(String groupUuid);
}