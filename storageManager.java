import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;

//TODO: DONT PUT N TO LAST LİNE
public class storageManager {
    public static HashSet<String> types = new HashSet<>();
    // {Cat:{234 : <a,b,c>}}
    public static HashMap<String, HashSet<Integer>> typesAndRecords = new HashMap<String, HashSet<Integer>>();

    public static void main(String[] args) throws IOException {
        //TODO: ÖNCEDEN VARMIŞŞŞŞŞ
        Constants.INPUT_FILE = args[0];
        Constants.OUTPUT_FILE = args[1];

        ArrayList<HashMap<String, String[]>> commandList = new ArrayList<>();

        File syscat = new File(Constants.SYS_CAT_FILE);
        File outputFile = new File(Constants.OUTPUT_FILE);

        syscat.createNewFile();
        outputFile.createNewFile();

        int commandCount = getCommands(commandList);
        for (int i = 0; i < Constants.FILE_COUNT; i++) {
            File file = new File("DataFile" + i);
            file.createNewFile();
        }

        executeCommands(commandList, syscat, outputFile);


    }


    public static int getCommands(ArrayList<HashMap<String, String[]>> commandList) throws IOException {
        File inputFile = new File(Constants.INPUT_FILE);
        Scanner sc = new Scanner(inputFile);
        int commandCount = 0;
        // < create type : jıjıj,jıskjsı>, < delete : jıoxsk xj>
        ArrayList<String> ss = new ArrayList<>();
        while (sc.hasNextLine()) {
            ss.add(sc.nextLine());
            commandCount++;

        }
        for (int i = 0; i < commandCount; i++) {
            HashMap<String, String[]> commandWithParams = new HashMap<>();
            String[] splitted = ss.get(i).split("\\s+", 3);
            String commandName = splitted[0] + splitted[1];
            if (splitted.length > 2) {
                String[] params = splitted[2].split("\\s+");
                commandWithParams.put(commandName, params);
                commandList.add(commandWithParams);

            } else {
                String[] params = new String[1];
                commandWithParams.put(commandName, params);
                commandList.add(commandWithParams);

            }

        }

        sc.close();

        return commandCount;
    }

    public static void executeCommands(ArrayList<HashMap<String, String[]>> commandList, File syscat, File output) throws IOException {
        for (int i = 0; i < commandList.size(); i++) {
            for (String methodName : commandList.get(i).keySet()) {
                if (methodName.equalsIgnoreCase("createtype")) {
                    createType(commandList.get(i).get(methodName), syscat);

                } else if (methodName.equalsIgnoreCase("deletetype")) {
                    deleteType(commandList.get(i).get(methodName), syscat);

                } else if (methodName.equalsIgnoreCase("listtype")) {
                    listAllTypes(commandList.get(i).get(methodName), output);
                } else if (methodName.equalsIgnoreCase("createrecord")) {
                    createRecord(commandList.get(i).get(methodName));
                } else if (methodName.equalsIgnoreCase("deleterecord")) {
                    deleteRecord(commandList.get(i).get(methodName));
                } else if (methodName.equalsIgnoreCase("updaterecord")) {
                    updateRecord(commandList.get(i).get(methodName));
                } else if (methodName.equalsIgnoreCase("searchrecord")) {
                    searchForRecord(commandList.get(i).get(methodName));
                } else if (methodName.equalsIgnoreCase("listrecord")) {
                    listAllRecords(commandList.get(i).get(methodName));
                } else {
                    System.out.println("Wrong Operation");
                }
            }
        }

    }


    public static void createType(String[] params, File syscat) throws IOException {
        if (!types.contains(params[0])) {
            types.add(params[0]);
            String typeSignature = "";
            for (int i = 0; i < params.length; i++) {
                if (i == 1) {
                    String str = params[i];
                    if (str.length() == 1) {
                        str += Constants.FILLER_DELIMETER;
                        typeSignature += str + Constants.FIELD_DELIMETER;
                    }

                } else {
                    String str = params[i];
                    for (int j = str.length(); j < Constants.FIELD_VAL; j++) {
                        str += Constants.FILLER_DELIMETER;
                    }
                    typeSignature += str + Constants.FIELD_DELIMETER;
                }
            }
            for (int i = params.length; i < Constants.NUM_OF_FIELDS + 2; i++) {
                typeSignature += "**********";
                if (i != 11) {
                    typeSignature += Constants.FIELD_DELIMETER;
                }
            }
            RandomAccessFile file = new RandomAccessFile(syscat, "rw");
            long filesize = file.length();
            file.seek(filesize);
            typeSignature += Constants.RECORD_DELIMETER;
            byte[] bArray = typeSignature.getBytes();
            file.write(bArray);
            file.close();
            HashSet<Integer> recordIds = new HashSet<>();
            typesAndRecords.put(params[0], recordIds);


        } else {
            System.out.println("Type already exists!");
        }
    }

    public static void deleteType(String[] params, File syscat) throws IOException {
        if (!types.contains(params[0])) {
            return;
        }
        String typeName = params[0];

        HashSet<Integer> records = typesAndRecords.get(typeName);
        RandomAccessFile rd = new RandomAccessFile(syscat, "rw");
        long syscatlen = rd.length();
        byte[] bArray = new byte[Constants.TYPE_SIZE];
        byte[] lastLine = new byte[Constants.TYPE_SIZE];
        long pageByte = Constants.PAGE_SIZE;
        long filesize = rd.length();
        int cursor = 0;
        int line = Constants.TYPE_SIZE;
        long pageNum = syscatlen / pageByte + 1;
        for (int j = 0; j < pageNum; j++) {
            for (int i = 0; i < types.size(); i++) {
                rd.seek(cursor);
                rd.read(bArray);
                String newString = new String(bArray);

                if (newString.substring(0, typeName.length()).equals(typeName)) {
                    if (cursor != (int) filesize - line) {
                        cursor = (int) filesize - line;
                        rd.seek(cursor);
                        rd.read(lastLine);
                        cursor = i * line;

                        rd.seek(cursor);
                        rd.write(lastLine);
                        rd.setLength(rd.length() - line);
                    } else {
                        rd.setLength(rd.length() - line);
                    }
                    break;
                } else {
                    cursor += line;
                }
            }

        }
        rd.close();
        types.remove(typeName);
        typesAndRecords.remove(typeName);
        for (Integer recordId : records) {
            int fileNum = recordId / (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
            File file = new File("File" + fileNum);
            RandomAccessFile datafile = new RandomAccessFile(file, "rw");
            int modId = recordId % (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
            int pageId = modId / Constants.RECORD_PER_PAGE;
            int pageModId = modId % Constants.RECORD_PER_PAGE;
            int cursor2 = pageId * Constants.PAGE_SIZE + pageModId * Constants.RECORD_SIZE + 11;
            byte[] emptyMarker = "0".getBytes(); // 0 means empty
            datafile.seek(cursor2);
            datafile.write(emptyMarker);

        }


    }

    public static void listAllTypes(String[] strings, File output) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(output, true);
        List<String> aList = new ArrayList<String>(types);
        Collections.sort(aList);
        String toWrite = "";
        for (int i = 0; i < aList.size(); i++) {
            String str = aList.get(i);
            byte[] toBytes = str.getBytes();
            outputStream.write(toBytes);
            outputStream.write("\n".getBytes());
        }

        outputStream.close();


    }

    public static void createRecord(String[] params) throws IOException {
        if (!types.contains(params[0])) {
            return;
        }
        int recordId = Integer.valueOf(params[1]);
        String recordSignature = "";
        for (int i = 1; i < params.length; i++) {
            String str = params[i];
            for (int j = str.length(); j < Constants.FIELD_VAL; j++) {
                str += Constants.FILLER_DELIMETER;

            }
            recordSignature += str + Constants.FIELD_DELIMETER;
            if (i == 1) {
                recordSignature += "1" + Constants.FIELD_DELIMETER;
            }

        }

        for (int i = params.length; i < Constants.NUM_OF_FIELDS + 1; i++) {
            recordSignature += "**********";
            if (i != Constants.NUM_OF_FIELDS) {
                recordSignature += Constants.FIELD_DELIMETER;
            }
        }
        recordSignature += Constants.RECORD_DELIMETER;

        int fileNum = recordId / (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
        File file = new File("File" + fileNum);
        RandomAccessFile datafile = new RandomAccessFile(file, "rw");
        int modId = recordId % (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
        int pageId = modId / Constants.RECORD_PER_PAGE;
        int pageModId = modId % Constants.RECORD_PER_PAGE;
        int cursor = pageId * Constants.PAGE_SIZE + pageModId * Constants.RECORD_SIZE;
        datafile.seek(cursor);
        byte[] recordBytes = recordSignature.getBytes();
        datafile.write(recordBytes);
        datafile.close();
        typesAndRecords.get(params[0]).add(recordId);


    }

    public static void deleteRecord(String[] strings) {

    }

    public static void updateRecord(String[] strings) {
    }

    public static void searchForRecord(String[] strings) {
    }

    public static void listAllRecords(String[] strings) {
    }

}