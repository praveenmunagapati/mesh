package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.UUIDUtil.isUUID;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;

import java.util.HashMap;
import java.util.Map;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.cailun.core.AbstractProjectRestVerticle;
import com.gentics.cailun.core.data.model.ObjectSchema;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.rest.schema.response.ObjectSchemaResponse;

@Component
@Scope("singleton")
@SpringVerticle
public class ObjectSchemaVerticle extends AbstractProjectRestVerticle {

	@Autowired
	private ObjectSchemaService schemaService;
	
	@Autowired
	private ProjectService projectService;

	protected ObjectSchemaVerticle() {
		super("schemas");
	}

	@Override
	public void registerEndPoints() throws Exception {
		addCRUDHandlers();
	}

	private void addCRUDHandlers() {

		addCreateHandler();
		addReadHandlers();
		addUpdateHandler();
		addDeleteHandler();

		addSchemaProjectHandlers();

	}

	private void addSchemaProjectHandlers() {
		route("/:schemaUUid/projects/:projectUuid").method(PUT).handler(rc -> {
			String schemaUUid = rc.request().params().get("schemaUUid");
			String projectUuid = rc.request().params().get("projectUuid");
			
			ObjectSchema schema = schemaService.findByUUID(schemaUUid);
			Project project = projectService.findByUUID(projectUuid);
		});

		route("/:schemaUUid/projects/:projectUuid").method(DELETE).handler(rc -> {
			String schemaUUid = rc.request().params().get("schemaUUid");
			String projectUuid = rc.request().params().get("projectUuid");
			
			ObjectSchema schema = schemaService.findByUUID(schemaUUid);
			Project project = projectService.findByUUID(projectUuid);
			
		});
	}

	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {

		});

	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			ObjectSchema schema = schemaService.findByUUID(uuid);

		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			ObjectSchema schema = schemaService.findByUUID(uuid);

		});

	}

	private void addReadHandlers() {
		// produces(APPLICATION_JSON)
		route("/").method(GET).handler(rc -> {
			String projectName = getProjectName(rc);
			Iterable<ObjectSchema> projectSchemas = schemaService.findAll(projectName);
			Map<String, ObjectSchemaResponse> resultMap = new HashMap<>();
			if (projectSchemas == null) {
				rc.response().end(toJson(resultMap));
				return;
			}
			for (ObjectSchema schema : projectSchemas) {
				ObjectSchemaResponse restSchema = schemaService.getReponseObject(schema);
				resultMap.put(schema.getName(), restSchema);
			}
			rc.response().end(toJson(resultMap));
			return;
		});

		route("/:uuid").method(GET).handler(rh -> {

			String projectName = getProjectName(rh);
			String uuidOrName = rh.request().params().get("uuidOrName");
			if (isUUID(uuidOrName)) {
				ObjectSchema projectSchema = schemaService.findByUUID(projectName, uuidOrName);
				rh.response().end(toJson(projectSchema));
				return;
			} else {
				ObjectSchema projectSchema = schemaService.findByName(projectName, uuidOrName);
				ObjectSchemaResponse schemaForRest = schemaService.getReponseObject(projectSchema);
				rh.response().end(toJson(schemaForRest));
				return;
			}

		});

	}

}
