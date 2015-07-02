package com.gentics.mesh.core.data.generic;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_FIELD_CONTAINER;

import com.gentics.mesh.core.data.BasicFieldContainer;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.Translated;
import com.gentics.mesh.core.data.impl.TranslatedImpl;
import com.syncleus.ferma.traversals.EdgeTraversal;

public class GenericFieldContainerNode extends AbstractGenericNode {

	protected <T extends BasicFieldContainer> T getFieldContainer(Language language, Class<T> classOfT) {
		T container = outE(HAS_FIELD_CONTAINER).has("languageTag", language.getLanguageTag()).inV().nextOrDefault(classOfT, null);
		return container;
	}

	/**
	 * Optionally creates a new field container for the given container and language.
	 * 
	 * @param language
	 * @return i18n properties vertex entity
	 */
	protected <T extends BasicFieldContainer> T getOrCreateFieldContainer(Language language, Class<T> classOfT) {

		T container = null;
		EdgeTraversal<?, ?, ?> edgeTraversal = outE(HAS_FIELD_CONTAINER).has(TranslatedImpl.LANGUAGE_TAG_KEY, language.getLanguageTag());
		if (edgeTraversal.hasNext()) {
			container = edgeTraversal.next().inV().has(classOfT).nextOrDefault(classOfT, null);
		}

		if (container == null) {
			container = getGraph().addFramedVertex(classOfT);
			container.setLanguage(language);
			Translated edge = addFramedEdge(HAS_FIELD_CONTAINER, container.getImpl(), TranslatedImpl.class);
			edge.setLanguageTag(language.getLanguageTag());
		}

		return container;
	}

	// public void addI18nProperties(I18NFieldContainer properties) {
	// linkOut(properties, HAS_I18N_PROPERTIES);
	// Translated edge = addFramedEdge(HAS_I18N_PROPERTIES, properties, Translated.class);
	// edge.setLanguageTag(properties.getLanguage().getLanguageTag());
	// }
	//

	// public void setI18NProperty(Language language, String name, String value) {
	// I18NFieldContainer properties = getOrCreateI18nProperties(language);
	// properties.setProperty(name, value);
	// }

	// public String getI18nProperty(Language language, String key) {
	// I18NFieldContainer properties = getI18nProperties(language);
	// if (properties == null) {
	// return null;
	// } else {
	// return properties.getProperty(key);
	// }
	// }

}