import java.util.Hashtable;
import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Set;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.StringBuilder;

class Property {
	public String val;
	public ArrayList<String> items;

	public void addItem(String item) {
	    this.items.add(item);
       }
}

class Section {
	public String handleLine;
	public String title;
	public Hashtable<String, Property> props;
}

public class Main {
    enum ParserState {
	NOOP,
	SECTIONNAME,
	READKEYVALUE,
	READLIST
    }
    
    public static int getIndentLevel(String line) {
	for (int i = 0; i < line.length(); i++) {
	    if (!Character.isWhitespace(line.charAt(i)))
		return i;
	}
	return 0;
    }

    public static Hashtable<String, Section> parseDMI(String source) {
	ParserState state = ParserState.NOOP;
	Stream<String> srcStream = source.lines();
	String[] lines = srcStream.toArray(String[]::new);
	Hashtable<String, Section> sects = new Hashtable<>();
	Section s = null;
	Property p = null;
	String k = "", v;

	for (int i = 0; i < lines.length; i++) {
	    if (lines[i].startsWith("Handle")) {
		s = new Section();
		s.props = new Hashtable<String, Property>();
		s.handleLine = lines[i];
		state = ParserState.SECTIONNAME;
		continue;
	    }
	    if (i == lines.length - 1) {
		if (s != null)
		    sects.put(s.title, s);
	        continue;
	    }
	    if (state == ParserState.SECTIONNAME) {
		s.title = lines[i];
		state = ParserState.READKEYVALUE;
	    } else if (state == ParserState.READKEYVALUE) {
		String[] pair = lines[i].split(":");
		k = pair[0].strip();
		if (pair.length == 2)
		    v = pair[1].strip();
		else
		    v = "";
		p = new Property();
		p.val = v;
		p.items = new ArrayList<String>();
		if (i < lines.length - 1 && (getIndentLevel(lines[i]) < getIndentLevel(lines[i+1])))
		    state = ParserState.READLIST;
		else
		    s.props.put(k, p);
	    } else if (state == ParserState.READLIST) {
		p.addItem(lines[i].strip());
		if (getIndentLevel(lines[i]) > getIndentLevel(lines[i+1])) {
		    state = ParserState.READKEYVALUE;
		    s.props.put(k, p);
		}
	    }
	}

	return sects;
    }
    
    public static void main(String[] args) throws IOException {
	String sample = """
# dmidecode 3.1
Getting SMBIOS data from sysfs.
SMBIOS 2.6 present.

Handle 0x0001, DMI type 1, 27 bytes
System Information
        Manufacturer: LENOVO
        Product Name: 20042
        Version: Lenovo G560
        Serial Number: 2677240001087
        UUID: CB3E6A50-A77B-E011-88E9-B870F4165734
        Wake-up Type: Power Switch
        SKU Number: Calpella_CRB
        Family: Intel_Mobile
	    """;

	if (args.length == 1) {
	    try (FileReader fr = new FileReader(args[0]);
		 BufferedReader br = new BufferedReader(fr)) {
		StringBuilder sb = new StringBuilder();
		String line = br.readLine();

		while (line != null) {
		    sb.append(line);
		    sb.append(System.lineSeparator());
		    line = br.readLine();
		}
		sample = sb.toString();
	    }
	}
	
	Hashtable<String, Section> obj = parseDMI(sample);

	Set<String> keySet = obj.keySet();
	for (String secname : keySet) {
	    Section sec = obj.get(secname);
	    System.out.println(secname + " with " + sec.props.size());
	    Set<String> propSet = sec.props.keySet();
	    for (String k : propSet) {
		Property p = sec.props.get(k);
		System.out.println("k: " + k + " => " + p.val);
		if (p.items.size() > 0) {
		    for (String i : p.items)
			System.out.println("\t\t I: " + i);
		}
	    }
	}
    }
}
