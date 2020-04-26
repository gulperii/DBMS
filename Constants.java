public class Constants {

    public static String INPUT_FILE;
    public static String OUTPUT_FILE;
    public static final String SYS_CAT_FILE = "SysCat.txt";

    public static final String RECORD_DELIMETER = "#";
    public static final String FIELD_DELIMETER = "-";
    public static final String FILLER_DELIMETER = "*";

    public static final int MAX_RECORD_COUNT = 10000;

    public static final int NUM_OF_FIELDS = 10;
    public static final int FIELD_VAL = 10;

    public static final int PAGE_HEADER_SIZE = 16; //+ 1
    public static final int PAGE_PER_FILE = 20;
    public static final int TYPE_PER_PAGE = 20;
    public static final int RECORD_PER_PAGE = 20;

    public static final int RECORD_SIZE = 123; //! with delimeter
    public static final int TYPE_SIZE = 124;
    public static final int PAGE_SIZE = 2500; // 16 + 20 * 123(122+1)
    public static final int FILE_SIZE = PAGE_PER_FILE * PAGE_SIZE; // in bytes

    public static final int FILE_COUNT = MAX_RECORD_COUNT / (PAGE_PER_FILE*RECORD_PER_PAGE);

}
