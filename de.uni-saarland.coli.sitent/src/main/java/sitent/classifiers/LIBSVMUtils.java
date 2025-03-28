package sitent.classifiers;

/**
 * Difference vs. CRF++ (?): CRF++ basically has two features for binary (0/1) features: one for "on" and one for "off".
 */

import java.io.FileNotFoundException;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

public class LIBSVMUtils {

	static Logger log = Logger.getLogger(CrfppUtils.class.getName());

	static DecimalFormat df = new DecimalFormat();
	static {
		df.setMaximumFractionDigits(10);
		df.setMinimumFractionDigits(1);
	}

	// map from features (=feature-value combinations) to indices
	private static Map<String, Integer> featureIndexMap = new HashMap<String, Integer>();
	private static int indexCounter = 0;

	/**
	 * Writes a Weka Instances data set to a file in LibSVM format.
	 * 
	 * 
	 * @param instances
	 *            A Weka Instances object.
	 * @param path
	 *            Path where to write the LIBSVM input file.
	 * @throws FileNotFoundException
	 */
	public static StringBuffer getLibSVMRepresentation(Instances instances, Set<String> trainDocs)
			throws FileNotFoundException {
		log.info("Getting LIBSVM representation: " + instances.numInstances());
		log.info("Train docs null? " + trainDocs == null);
		int x = 0;
		StringBuffer content = new StringBuffer();

		// Map<String, Set<String>> singleValuedAttributes = new HashMap<String,
		// Set<String>>();

		instancesLoop: for (int j = 0; j < instances.numInstances(); j++) {
			if (j % 10000 == 0) {
				log.info("writing input for LIBSVM... " + j);
			}
			String line = "";
			Instance inst = instances.instance(j);
			// so they can be sorted (otherwise LIBLINEAR dies)
			Map<Integer, String> featureFunctionValues = new HashMap<Integer, String>();

			// get document Id
			Attribute instIdAttr = instances.attribute("instanceid");
			String instId = inst.stringValue(instIdAttr);
			String documentId = instId.substring(0, instId.lastIndexOf("_"));

			if (trainDocs != null) {
				// skip instance if from a document that should not be used in
				// training
				// System.out.println("Document id: " + documentId);
				if (!trainDocs.contains(documentId)) {
					// System.out.println("skipping: " + documentId);
					continue instancesLoop;
				}
				// System.out.println("using: " + documentId);
			}

			// class label: value index!
			line += new Double(inst.value(inst.classIndex())).intValue();
			String endOfLine = "";

			for (int k = 0; k < instances.numAttributes(); k++) {
				// log.info("Feature: " + instances.attribute(k).name() + " with
				// value " + inst.value(k));
				if (k != instances.classIndex()) {
					String val;
					if (instances.attribute(k).isNumeric()) {
						/*
						 * // for "binary" features (1/0): if (inst.value(k) ==
						 * 0) { // feature off endOfLine += " " + (k +
						 * instances.numAttributes()) + ":1.0"; } else { //
						 * value should be 1.0 // feature on line += " " + k +
						 * ":1.0"; }
						 */

						// feature index for this numeric feature
						if (!featureIndexMap.containsKey(instances.attribute(k).name())) {
							indexCounter++;
							featureIndexMap.put(instances.attribute(k).name(), indexCounter);
						}
						Integer index = featureIndexMap.get(instances.attribute(k).name());

						try {
							val = df.format(inst.value(k)).replaceAll(",", "");// new
							// BigDecimal(inst.value(k)).toPlainString();
						} catch (NumberFormatException e) {
							val = "0.0";
						}
						// line += " " + index + ":" + val;
						featureFunctionValues.put(index, val);

					} else {
						if (instances.attribute(k).name().equals("instanceid")) {
							continue;
						}

						// create indices for non-numeric features --> indicator
						// functions
						String featureFunction = instances.attribute(k).name() + "==" + inst.value(k);
						if (!featureIndexMap.containsKey(featureFunction)) {
							indexCounter++;
							featureIndexMap.put(featureFunction, indexCounter);
						}
						Integer index = featureIndexMap.get(featureFunction);
						// line += " " + index + ":1.0";
						featureFunctionValues.put(index, "1.0");

						// log.error("LIBSVM can only handle numeric
						// features!");
						// log.error("Trying to use feature: " +
						// instances.attribute(k).name() + " with value " +
						// inst.value(k));
						// for (int q=0; q<instances.attribute(k).numValues();
						// q++) {
						// System.out.println("- value: " +
						// instances.attribute(k).value(q));
						// }
						//
						// throw new IllegalStateException();

					}
				}

			}
			// sort keyset
			List<Integer> featureIndices = new LinkedList<Integer>(featureFunctionValues.keySet());
			Collections.sort(featureIndices);
			for (int index : featureIndices) {
				line += " " + index + ":" + featureFunctionValues.get(index);
			}

			content.append(line + " " + endOfLine + "\n");
			x++;
		}
		log.info("wrote " + x + " lines");
		return content;
	}

}
