import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.RandomAccessFile;
import java.util.*;


public class storageManager {
    public static File syscat;
    public static File index;

    // Internal map to to store types and record keys of the records of that type
    public static HashMap<String, TreeSet<Integer>> typesAndRecords = new HashMap<String, TreeSet<Integer>>();

    public static void main(String[] args) throws IOException {
        Constants.INPUT_FILE = args[0];
        Constants.OUTPUT_FILE = args[1];

        ArrayList<HashMap<String, String[]>> commandList = new ArrayList<>();

        syscat = new File(Constants.SYS_CAT_FILE);
        index = new File(Constants.INDEX_FILE);
        File outputFile = new File(Constants.OUTPUT_FILE);
        //if this is the first time the program is runned, it creates index file and syscat file
        if (!syscat.exists()) {
            syscat.createNewFile();
            index.createNewFile();
        } else {
            //If an old index file exists, it reads and stores it
            getExistingTypes();
            outputFile.delete();

        }
        outputFile.createNewFile();
        getCommands(commandList);
        executeCommands(commandList, syscat, outputFile);
        //save the types and records into index file
        saveToIndexFile();
    }

    //If a previous index file exists, we restore it back
    public static void getExistingTypes() throws IOException {
        File index = new File(Constants.INDEX_FILE);
        Scanner sc = new Scanner(index);
        ArrayList<String> ss = new ArrayList<>();
        int counter = 0;

        while (sc.hasNextLine()) {
            ss.add(sc.nextLine());
            counter++;
        }
        for (int i = 0; i < counter; i++) {
            String[] splitted = ss.get(i).split("#");
            for (int j = 0; j < splitted.length; j++) {
                String[] split = splitted[i].split("-");
                TreeSet<Integer> recordIds = new TreeSet<Integer>();
                for (int h = 1; h < split.length; h++) {
                    recordIds.add(Integer.valueOf(split[h]));
                }
                typesAndRecords.put(split[0], recordIds);
            }
        }


    }

    // At the end of the program we save the types and keys of records of that type into index file for further use
    public static void saveToIndexFile() throws IOException {
        File index = new File(Constants.INDEX_FILE);
        FileOutputStream outputStream = new FileOutputStream(index, false);
        String toWrite = "";
        for (String key : typesAndRecords.keySet()) {
            String typeSignature = key;
            for (Integer recId : typesAndRecords.get(key)) {
                // Field delimeter
                typeSignature += "-" + String.valueOf(recId);
            }
            // record delimeter
            typeSignature += "#";
            toWrite += typeSignature;
        }
        byte[] write = toWrite.getBytes();
        outputStream.write(write);
        outputStream.close();
    }

    //Parse the input file
    public static void getCommands(ArrayList<HashMap<String, String[]>> commandList) throws IOException {
        File inputFile = new File(Constants.INPUT_FILE);
        Scanner sc = new Scanner(inputFile);
        int commandCount = 0;
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
    }

    public static void executeCommands(ArrayList<HashMap<String, String[]>> commandList, File syscat, File output) throws IOException {
        for (int i = 0; i < commandList.size(); i++) {
            for (String methodName : commandList.get(i).keySet()) {
                if (methodName.equalsIgnoreCase("createtype")) {
                    createType(commandList.get(i).get(methodName), syscat);
                } else if (methodName.equalsIgnoreCase("deletetype")) {
                    deleteType(commandList.get(i).get(methodName), syscat);
                } else if (methodName.equalsIgnoreCase("listtype")) {
                    listAllTypes(syscat, output);
                } else if (methodName.equalsIgnoreCase("createrecord")) {
                    createRecord(commandList.get(i).get(methodName));
                } else if (methodName.equalsIgnoreCase("deleterecord")) {
                    deleteRecord(commandList.get(i).get(methodName));
                } else if (methodName.equalsIgnoreCase("updaterecord")) {
                    updateRecord(commandList.get(i).get(methodName));
                } else if (methodName.equalsIgnoreCase("searchrecord")) {
                    searchForRecord(commandList.get(i).get(methodName), output);
                } else if (methodName.equalsIgnoreCase("listrecord")) {
                    listAllRecords(commandList.get(i).get(methodName), output);
                } else {
                    System.out.println("Wrong Operation");
                }
            }
        }
        //To delete the trailing new line
        RandomAccessFile rd = new RandomAccessFile(output, "rw");
        rd.setLength(rd.length() - ("\n".getBytes()).length);
        rd.close();

    }


    public static void createType(String[] params, File syscat) throws IOException {
        String typeSignature = "";
        for (int i = 0; i < params.length; i++) {
            // To keep the record size fixed, if number of fields is less than 10 we add filler delimiter(*) to make it 2 bytes
            if (i == 1) {
                String str = params[i];
                if (str.length() == 1) {
                    str += Constants.FILLER_DELIMETER;
                }
                typeSignature += str + Constants.FIELD_DELIMETER;


            } else {
                String str = params[i];
                for (int j = str.length(); j < Constants.FIELD_VAL; j++) {
                    str += Constants.FILLER_DELIMETER;
                }
                if (i == 11) {
                    typeSignature += str;
                } else {
                    typeSignature += str + Constants.FIELD_DELIMETER;
                }
            }
        }
        // If number of fields are less than 10, we fill the rest with a 10 char-length string.
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

        TreeSet<Integer> recordIds = new TreeSet<>();
        typesAndRecords.put(params[0], recordIds);

    }

    public static void deleteType(String[] params, File syscat) throws IOException {

        String typeName = params[0];
        TreeSet<Integer> records = typesAndRecords.get(typeName);
        RandomAccessFile rd = new RandomAccessFile(syscat, "rw");
        long syscatlen = rd.length();
        byte[] bArray = new byte[Constants.TYPE_SIZE];
        byte[] lastLine = new byte[Constants.TYPE_SIZE];
        long pageByte = Constants.PAGE_SIZE;
        long filesize = rd.length();
        int cursor = 0;
        int line = Constants.TYPE_SIZE;
        long pageNum = syscatlen / pageByte + 1;
        // We read the syscat page by page to find the type name
        for (int j = 0; j < pageNum; j++) {
            for (int i = 0; i < 20; i++) {
                rd.seek(cursor);
                rd.read(bArray);
                String newString = new String(bArray);
                if (newString.substring(0, typeName.length()).equals(typeName)) {
                    // If we find it, we replace the type name we want to delete with the last type in the syscat.
                    if (cursor != (int) filesize - line) {
                        cursor = (int) filesize - line;
                        rd.seek(cursor);
                        rd.read(lastLine);
                        cursor = i * line;

                        rd.seek(cursor);
                        rd.write(lastLine);
                        rd.setLength(rd.length() - line);
                    } else {
                        // If it is already the last type, we simply shorten the length of syscat
                        rd.setLength(rd.length() - line);
                    }
                    break;
                } else {
                    cursor += line;
                }
            }

        }
        rd.close();
        if (records == null) {
            return;
        }
        // From the index file, we take the keys of the records of that type and delete them
        for (Integer recordId : records) {
            int fileNum = recordId / (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
            File file = new File("DataFile" + fileNum);
            RandomAccessFile datafile = new RandomAccessFile(file, "rw");
            int modId = recordId % (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
            int pageId = modId / Constants.RECORD_PER_PAGE;
            int pageModId = modId % Constants.RECORD_PER_PAGE;
            int cursor2 = pageId * Constants.PAGE_SIZE + pageModId * Constants.RECORD_SIZE + 11;
            byte[] emptyMarker = "0".getBytes(); // 0 means empty
            datafile.seek(cursor2);
            datafile.write(emptyMarker);


            cursor2 = Constants.PAGE_SIZE - 3;
            byte[] recordCount = new byte[3];
            datafile.seek(cursor2);
            datafile.read(recordCount);
            int recordC = (Integer.valueOf(new String(recordCount)));
            recordC -= 1;

            int cursor3 = Constants.PAGE_SIZE - 3;
            datafile.seek(cursor3);
//Decrement the number of records in the file by one
            if (recordC >= 1 && recordC <= 9) {
                String newRec = "00" + String.valueOf(recordC);
                recordCount = newRec.getBytes();
                datafile.write(recordCount);
                datafile.close();


            } else if (recordC >= 10 && recordC <= 99) {
                String newRec = "0" + String.valueOf(recordC);
                recordCount = newRec.getBytes();
                datafile.write(recordCount);
                datafile.close();


            } else if (recordC >= 100 && recordC <= 400) {
                String newRec = String.valueOf(recordC);
                recordCount = newRec.getBytes();
                datafile.write(recordCount);
                datafile.close();


            } else {
                datafile.close();
                file.delete();
            }
        }
        typesAndRecords.remove(typeName);


    }

    public static void listAllTypes(File syscat, File output) throws IOException {
        RandomAccessFile rd = new RandomAccessFile(syscat, "rw");
        long syscatlen = rd.length();
        byte[] typeName = new byte[10];
        long pageByte = Constants.PAGE_SIZE;
        int cursor = 0;
        int line = Constants.TYPE_SIZE;
        long pageNum = syscatlen / pageByte + 1;
        HashSet<String> aset = new HashSet<>();
        //We read the syscat page by page and read the type names
        for (int j = 0; j < pageNum; j++) {
            for (int i = 0; i < 20; i++) {
                if (cursor > syscatlen) {
                    break;
                }
                rd.seek(cursor);
                rd.read(typeName);
                String newString = new String(typeName);
                String newTypeName = newString.substring(0, Constants.FIELD_VAL);
                newTypeName = newTypeName.replace("*", "");
                aset.add(newTypeName);
                cursor = (i + 1) * line + j * Constants.PAGE_SIZE;

            }
        }
        rd.close();
        if (aset.isEmpty()) {
            return;
        }
        FileOutputStream outputStream = new FileOutputStream(output, true);
        ArrayList<String> aList = new ArrayList<>(aset);
        Collections.sort(aList);
        for (int i = 0; i < aList.size(); i++) {
            String str = aList.get(i);
            byte[] toBytes = str.getBytes();
            outputStream.write(toBytes);
            outputStream.write("\n".getBytes());
        }

        outputStream.close();
    }

    public static void createRecord(String[] params) throws IOException {
        int recordId = Integer.valueOf(params[1]);
        String recordSignature = "";
        // We make the necessary additions to keep the record size fixed by adding special delimiters.
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
        File file = new File("DataFile" + fileNum);
        boolean newlyCreated = !file.exists();

        RandomAccessFile datafile = new RandomAccessFile(file, "rw");
        // We calculate the address of the record according to its key. Please refer to project report for further info.
        int modId = recordId % (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
        int pageId = modId / Constants.RECORD_PER_PAGE;
        int pageModId = modId % Constants.RECORD_PER_PAGE;
        int cursor = pageId * Constants.PAGE_SIZE + pageModId * Constants.RECORD_SIZE;
        datafile.seek(cursor);
        byte[] recordBytes = recordSignature.getBytes();
        datafile.write(recordBytes);
// If thats the first record in the file, we set the number of records in the file to 1
        if (newlyCreated) {
            String numOfrecords = "001";
            byte[] recordCount = numOfrecords.getBytes();
            cursor = (int) Constants.PAGE_SIZE - recordCount.length;
            datafile.seek(cursor);
            datafile.write(recordCount);
        } else {
            // Else, we increment the record count by 1
            int cursor2 = Constants.PAGE_SIZE - 3;
            byte[] recordCount = new byte[3];
            datafile.seek(cursor2);
            datafile.read(recordCount);
            int recordC = (Integer.valueOf(new String(recordCount)));
            recordC += 1;

            int cursor3 = Constants.PAGE_SIZE - 3;
            datafile.seek(cursor3);

            if (recordC >= 1 && recordC <= 9) {
                String newRec = "00" + String.valueOf(recordC);
                recordCount = newRec.getBytes();
                datafile.write(recordCount);


            } else if (recordC >= 10 && recordC <= 99) {
                String newRec = "0" + String.valueOf(recordC);
                recordCount = newRec.getBytes();
                datafile.write(recordCount);

            } else if (recordC >= 100 && recordC <= 400) {
                String newRec = String.valueOf(recordC);
                recordCount = newRec.getBytes();
                datafile.write(recordCount);

            }

        }
        datafile.close();
        typesAndRecords.get(params[0]).add(recordId);


    }

    public static void deleteRecord(String[] strings) throws IOException {
        // Calculate teh address
        int recordId = Integer.valueOf(strings[1]);
        int fileNum = recordId / (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
        File file = new File("DataFile" + fileNum);
        RandomAccessFile datafile = new RandomAccessFile(file, "rw");
        int modId = recordId % (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
        int pageId = modId / Constants.RECORD_PER_PAGE;
        int pageModId = modId % Constants.RECORD_PER_PAGE;
        int cursor2 = pageId * Constants.PAGE_SIZE + pageModId * Constants.RECORD_SIZE + 11;
        byte[] emptyMarker = "0".getBytes(); // 0 means empty
        datafile.seek(cursor2);
        datafile.write(emptyMarker);

// Decrement the record count in the file
        cursor2 = Constants.PAGE_SIZE - 3;
        byte[] recordCount = new byte[3];
        datafile.seek(cursor2);
        datafile.read(recordCount);
        int recordC = (Integer.valueOf(new String(recordCount)));
        recordC -= 1;

        int cursor3 = Constants.PAGE_SIZE - 3;
        datafile.seek(cursor3);

        if (recordC >= 1 && recordC <= 9) {
            String newRec = "00" + String.valueOf(recordC);
            recordCount = newRec.getBytes();
            datafile.write(recordCount);
            datafile.close();


        } else if (recordC >= 10 && recordC <= 99) {
            String newRec = "0" + String.valueOf(recordC);
            recordCount = newRec.getBytes();
            datafile.write(recordCount);
            datafile.close();


        } else if (recordC >= 100 && recordC <= 400) {
            String newRec = String.valueOf(recordC);
            recordCount = newRec.getBytes();
            datafile.write(recordCount);
            datafile.close();


        } else {
            // If record count is 0, delete the file
            datafile.close();
            file.delete();
        }
        typesAndRecords.get(strings[0]).remove(recordId);


    }

    public static void updateRecord(String[] params) throws IOException {
// Make the modifications for fixed size
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
// Calculate the address
        int fileNum = recordId / (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
        File file = new File("DataFile" + fileNum);
        RandomAccessFile datafile = new RandomAccessFile(file, "rw");
        int modId = recordId % (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
        int pageId = modId / Constants.RECORD_PER_PAGE;
        int pageModId = modId % Constants.RECORD_PER_PAGE;
        int cursor = pageId * Constants.PAGE_SIZE + pageModId * Constants.RECORD_SIZE;
        datafile.seek(cursor);
        byte[] recordBytes = recordSignature.getBytes();
        datafile.write(recordBytes);
        datafile.close();


    }

    public static void searchForRecord(String[] strings, File output) throws IOException {
        int recordId = Integer.valueOf(strings[1]);
        int fileNum = recordId / (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
        File file = new File("DataFile" + fileNum);
        if (!file.exists()) {
            file.delete();
            return;
        }
        // Calculate the adress
        RandomAccessFile datafile = new RandomAccessFile(file, "rw");
        int modId = recordId % (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
        int pageId = modId / Constants.RECORD_PER_PAGE;
        int pageModId = modId % Constants.RECORD_PER_PAGE;
        int cursor = pageId * Constants.PAGE_SIZE + pageModId * Constants.RECORD_SIZE;
        byte[] recordParams = new byte[Constants.RECORD_SIZE];
        datafile.seek(cursor);
        datafile.read(recordParams);
        datafile.close();
        String recordSignature = new String(recordParams, "UTF-8"); // for UTF-8 encoding
        // Split the string by field delimiter
        String[] splittedParams = recordSignature.split(Constants.FIELD_DELIMETER);
        String toWrite = "";
        for (int i = 0; i < splittedParams.length; i++) {
            if (splittedParams[i].equals("0")) {
                return;
            }
            if (i != 1) {
                String str = splittedParams[i];
                if (!str.equals("**********")) {
                    str = str.replace(Constants.FILLER_DELIMETER, "");
                    str = str.replace(Constants.RECORD_DELIMETER, "");
                    toWrite += str + " ";

                }
            }
        }
        FileOutputStream outputStream = new FileOutputStream(output, true);
        toWrite = toWrite.substring(0, toWrite.length() - 1);
        byte[] toBytes = toWrite.getBytes();
        outputStream.write(toBytes);
        outputStream.write("\n".getBytes());
        outputStream.close();

    }

    public static void listAllRecords(String[] strings, File output) throws IOException {
        TreeSet<Integer> records = typesAndRecords.get(strings[0]);
        if (records == null) {
            return;
        }
        if (records.isEmpty()) {
            return;
        }
        for (Integer recordId : records) {
            int fileNum = recordId / (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
            File file = new File("DataFile" + fileNum);
            RandomAccessFile datafile = new RandomAccessFile(file, "rw");
            // Calculate the address
            int modId = recordId % (Constants.PAGE_PER_FILE * Constants.RECORD_PER_PAGE);
            int pageId = modId / Constants.RECORD_PER_PAGE;
            int pageModId = modId % Constants.RECORD_PER_PAGE;
            int cursor2 = pageId * Constants.PAGE_SIZE + pageModId * Constants.RECORD_SIZE;
            byte[] recordParams = new byte[Constants.RECORD_SIZE];
            datafile.seek(cursor2);
            datafile.read(recordParams);

            FileOutputStream outputStream = new FileOutputStream(output, true);
            String recordSignature = new String(recordParams, "UTF-8");
            // Split the string by field delimeter
            String[] splittedParams = recordSignature.split(Constants.FIELD_DELIMETER);
            String toWrite = "";
            for (int i = 0; i < splittedParams.length; i++) {
                if (i != 1) {
                    String str = splittedParams[i];
                    if (!str.equals("**********")) {
                        str = str.replace(Constants.FILLER_DELIMETER, "");
                        str = str.replace(Constants.RECORD_DELIMETER, "");
                        toWrite += str + " ";

                    }
                }
            }
            datafile.close();

            toWrite = toWrite.substring(0, toWrite.length() - 1);
            byte[] toBytes = toWrite.getBytes();
            outputStream.write(toBytes);
            outputStream.write("\n".getBytes());
            outputStream.close();

        }
    }

}