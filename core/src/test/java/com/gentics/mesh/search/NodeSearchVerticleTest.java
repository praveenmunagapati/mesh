package com.gentics.mesh.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.codehaus.jettison.json.JSONException;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.list.GraphStringFieldList;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.Future;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodeSearchVerticleTest extends AbstractSearchVerticleTest {

	private static final Logger log = LoggerFactory.getLogger(NodeSearchVerticleTest.class);

	@Autowired
	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(nodeVerticle);
		return list;
	}

	@Test
	public void testSearchAndSort() throws InterruptedException {
		fullIndex();

		String json = "{";
		json += "				\"sort\" : {";
		json += "			      \"created\" : {\"order\" : \"asc\"}";
		json += "			    },";
		json += "			    \"query\":{";
		json += "			        \"bool\" : {";
		json += "			            \"must\" : {";
		json += "			                \"term\" : { \"schema.name\" : \"content\" }";
		json += "			            }";
		json += "			        }";
		json += "			    }";
		json += "			}";

		Future<NodeListResponse> future = getClient().searchNodes(json);
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

		long lastCreated = 0;
		for (NodeResponse nodeResponse : response.getData()) {
			if (lastCreated > nodeResponse.getCreated()) {
				fail("Found entry that was not sorted by create timestamp. Last entry: {" + lastCreated + "} current entry: {"
						+ nodeResponse.getCreated() + "}");
			} else {
				lastCreated = nodeResponse.getCreated();
			}
			assertEquals("content", nodeResponse.getSchema().getName());
		}
	}

	@Test
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		fullIndex();

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("Concorde"), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(2, response.getData().size());
		deleteNode(DemoDataProvider.PROJECT_NAME, content("concorde").getUuid());

		future = getClient().searchNodes(getSimpleQuery("Concorde"), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals("We added the delete action and therefore the document should no longer be part of the index.", 0, response.getData().size());

	}

	@Test
	public void testBogusQuery() {
		Future<NodeListResponse> future = getClient().searchNodes("bogus}J}son");
		latchFor(future);
		expectException(future, BAD_REQUEST, "search_query_not_parsable");
	}

	@Test
	public void testCustomQuery() throws InterruptedException, JSONException {
		try (Trx tx = db.trx()) {
			boot.meshRoot().getSearchQueue().addFullIndex();
			tx.success();
		}

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleTermQuery("schema.name", "content"));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

	}

	@Test
	@Override
	public void testDocumentCreation() throws InterruptedException, JSONException {
		try (Trx tx = db.trx()) {
			Node node = folder("2015");

			GraphStringFieldList list = node.getGraphFieldContainer(english()).createStringList("stringList");
			list.createString("one");
			list.createString("two");
			list.createString("three");
			list.createString("four");

			Schema schema = node.getSchemaContainer().getSchema();
			schema.addField(new ListFieldSchemaImpl().setListType("string").setName("stringList"));
			node.getSchemaContainer().setSchema(schema);
			tx.success();
		}
		// Invoke a dummy search on an empty index
		String json = "{";
		json += "				\"sort\" : {";
		json += "			      \"created\" : {\"order\" : \"asc\"}";
		json += "			    },";
		json += "			    \"query\":{";
		json += "			        \"bool\" : {";
		json += "			            \"must\" : {";
		json += "			                \"term\" : { \"fields.stringList\" : \"three\" }";
		json += "			            }";
		json += "			        }";
		json += "			    }";
		json += "			}";

		Future<NodeListResponse> future = getClient().searchNodes(json, new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(0, response.getData().size());

		// Create the update entry in the search queue
		try (Trx tx = db.trx()) {
			SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
			SearchQueueBatch batch = searchQueue.createBatch("0");
			Node node = folder("2015");
			batch.addEntry(node.getUuid(), Node.TYPE, SearchQueueEntryAction.CREATE_ACTION);
			tx.success();
		}
		// CountDownLatch latch = new CountDownLatch(1);
		// vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, true, new DeliveryOptions().setSendTimeout(100000L), rh -> {
		// latch.countDown();
		// });
		// failingLatch(latch);

		// Search again and make sure we found our document
		future = getClient().searchNodes(json, new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(
				"There should be at least one item in the resultset since we added the search queue entry and the index should now contain this item.",
				1, response.getData().size());

	}

	@Test
	@Override
	public void testDocumentUpdate() throws InterruptedException, JSONException {
		fullIndex();

		String newString = "ABCDEFGHI";
		Node node;
		SearchQueueBatch batch;
		try (Trx tx = db.trx()) {
			node = content("concorde");
			assertNotNull(node);
			HtmlGraphField field = node.getGraphFieldContainer(english()).getHtml("content");
			assertNotNull(field);
			field.setHtml(newString);

			// Create the update entry in the search queue
			batch = boot.meshRoot().getSearchQueue().createBatch("0");
			batch.addEntry(node.getUuid(), Node.TYPE, SearchQueueEntryAction.UPDATE_ACTION, Node.TYPE + "-en");
			batch.addEntry(node.getUuid(), Node.TYPE, SearchQueueEntryAction.UPDATE_ACTION, Node.TYPE + "-de");
			tx.success();
		}

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("supersonic"), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(1, response.getData().size());

		try (Trx tx = db.trx()) {
			CountDownLatch latch = new CountDownLatch(1);
			batch.process(rh -> {
				latch.countDown();
			});
			failingLatch(latch);
		}

		future = getClient().searchNodes(getSimpleQuery("supersonic"), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals("The node with name {" + "Concorde" + "} should no longer be found since we updated the node and updated the content.", 0,
				response.getData().size());

		future = getClient().searchNodes(getSimpleQuery(newString), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals("There should be one item in the resultset since we updated the node and invoked the index update.", 1,
				response.getData().size());

	}

	@Test
	public void testSearchContent() throws InterruptedException, JSONException {
		fullIndex();

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("the"), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(1, response.getData().size());
		assertEquals(1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}

	}
}