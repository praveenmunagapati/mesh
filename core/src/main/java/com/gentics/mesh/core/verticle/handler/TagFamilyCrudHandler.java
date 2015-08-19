package com.gentics.mesh.core.verticle.handler;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getPagingInfo;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.loadTransformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.ext.web.RoutingContext;

@Component
public class TagFamilyCrudHandler extends AbstractCrudHandler {

	@Override
	public void handleCreate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			MeshAuthUser requestUser = getUser(rc);
			TagFamilyCreateRequest requestModel = fromJson(rc, TagFamilyCreateRequest.class);

			String name = requestModel.getName();
			if (StringUtils.isEmpty(name)) {
				rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tagfamily_name_not_set")));
			} else {
				if (project.getTagFamilyRoot().findByName(name) != null) {
					rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "tagfamily_conflicting_name", name)));
					return;
				}

				TagFamilyRoot root = project.getTagFamilyRoot();
				/* TODO check for null */
				if (requestUser.hasPermission(root, CREATE_PERM)) {
					TagFamily tagFamily = null;
					try (Trx txCreate = new Trx(db)) {
						tagFamily = root.create(name, requestUser);
						root.addTagFamily(tagFamily);
						requestUser.addCRUDPermissionOnRole(root, CREATE_PERM, tagFamily);
						txCreate.success();
					}
					transformAndResponde(rc, tagFamily);
				} else {
					rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", root.getUuid())));
				}
			}
		}

	}

	@Override
	public void handleDelete(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			delete(rc, "uuid", "tagfamily_deleted", getProject(rc).getTagFamilyRoot());
		}
	}

	@Override
	public void handleUpdate(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			TagFamilyUpdateRequest requestModel = fromJson(rc, TagFamilyUpdateRequest.class);

			String newName = requestModel.getName();
			if (StringUtils.isEmpty(newName)) {
				rc.fail(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tagfamily_name_not_set")));
			} else {
				loadObject(rc, "uuid", UPDATE_PERM, project.getTagFamilyRoot(), rh -> {
					if (hasSucceeded(rc, rh)) {
						TagFamily tagFamilyWithSameName = project.getTagFamilyRoot().findByName(newName);
						TagFamily tagFamily = rh.result();
						if (tagFamilyWithSameName != null && !tagFamilyWithSameName.getUuid().equals(tagFamily.getUuid())) {
							rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "tagfamily_conflicting_name", newName)));
							return;
						}
						try (Trx txUpdate = new Trx(db)) {
							tagFamily.setName(newName);
							txUpdate.success();
						}
						transformAndResponde(rc, tagFamily);
					}
				});
			}
		}
	}

	@Override
	public void handleRead(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			loadTransformAndResponde(rc, "uuid", READ_PERM, getProject(rc).getTagFamilyRoot());
		}
	}

	@Override
	public void handleReadList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			loadTransformAndResponde(rc, project.getTagFamilyRoot(), new TagFamilyListResponse());
		}
	}

	public void handleReadTagList(RoutingContext rc) {
		try (Trx tx = new Trx(db)) {

			Project project = getProject(rc);
			MeshAuthUser requestUser = getUser(rc);
			PagingInfo pagingInfo = getPagingInfo(rc);

			// TODO this is not checking for the project name and project relationship. We _need_ to fix this!
			loadObject(rc, "tagFamilyUuid", READ_PERM, project.getTagFamilyRoot(), rh -> {
				if (hasSucceeded(rc, rh)) {
					TagFamily tagFamily = rh.result();
					try {
						Page<? extends Tag> tagPage = tagFamily.getTags(requestUser, pagingInfo);
						transformAndResponde(rc, tagPage, new TagListResponse());
					} catch (Exception e) {
						rc.fail(e);
					}
				}
			});
		}
	}
}