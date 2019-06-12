package Dependencies;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

// ORIGINAL AUTHOR: http://javatechniques.com/blog/faster-deep-copies-of-java-objects/
public class DeepCopy {

	/**
	 * Returns a copy of the object, or null if the object cannot
	 * be serialized.
	 */
	public static Object copy(Object orig) {
		Object obj = null;
		try {
			// Write the object out to a byte array
			FastByteArrayOutputStream fbos =
					new FastByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(fbos);
			out.writeObject(orig);
			out.flush();
			out.close();

			// Retrieve an input stream from the byte array and read
			// a copy of the object back in.
			ObjectInputStream in =
					new ObjectInputStream(fbos.getInputStream());
			obj = in.readObject();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return obj;
	}
}