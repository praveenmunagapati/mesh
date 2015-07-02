package com.gentics.mesh.core.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class NodeFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleNodeField() {

		Node node = fg.addFramedVertexExplicit(NodeImpl.class);

		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		NodeField field = container.createNode("testNodeField", node);
		Node loadedNode =field.getNode();
		assertNotNull(loadedNode);
		assertEquals(node.getUuid(), loadedNode.getUuid());
		
		NodeField loadedField = container.getNode("testNodeField");
		assertNotNull(loadedField);
		assertNotNull(loadedField.getNode());
		assertEquals(node.getUuid(), loadedField.getNode());

	}
}