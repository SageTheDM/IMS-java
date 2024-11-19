import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class App {
    // #region Constants
    private static final Scanner scanner = new Scanner(System.in);
    private static final int ROOM_COUNT = 3;
    private static final int DAY_COUNT = 5;
    private static final int LESSON_COUNT = 12;

    public static final Lesson[][][] timeTable = new Lesson[ROOM_COUNT][DAY_COUNT][LESSON_COUNT];
    private static final Teacher[] teachers = new Teacher[Teacher.nameMap.size()];

    // Date constants
    private static final int START_YEAR = 2024;
    private static final int START_MONTH = 11; // November
    private static final int START_DAY = 4;
    private static final String START_DATE = String.format("%d-%02d-%02d%%2000:00:00", START_YEAR, START_MONTH,
            START_DAY);

    private static final int END_YEAR = 2024;
    private static final int END_MONTH = 11; // November
    private static final int END_DAY = 8;
    private static final String END_DATE = String.format("%d-%02d-%02d%%2000:00:00", END_YEAR, END_MONTH, END_DAY);

    // Room channel numbers
    private static final int ROOM_39_NUMBER = 1521262;
    private static final int ROOM_38_NUMBER = 1364580;
    private static final int ROOM_37_NUMBER = 1521263;

    // Room URLs
    private static final String ROOM_39_URL = createUrl(ROOM_39_NUMBER);
    private static final String ROOM_38_URL = createUrl(ROOM_38_NUMBER);
    private static final String ROOM_37_URL = createUrl(ROOM_37_NUMBER);

    // Room data
    private static final List<Co2Data> ROOM_39_DATA = Co2Data.getData(ROOM_39_URL, 39);
    private static final List<Co2Data> ROOM_38_DATA = Co2Data.getData(ROOM_38_URL, 38);
    private static final List<Co2Data> ROOM_37_DATA = Co2Data.getData(ROOM_37_URL, 37);

    private static String createUrl(int channelNumber) {
        return String.format("https://api.thingspeak.com/channels/%d/feeds.csv?start=%s&end=%s", channelNumber,
                START_DATE, END_DATE);
    }

    // #region Initialization
    private static void initializeTeachers() {
        int index = 0;
        for (String initial : Teacher.nameMap.keySet()) {
            teachers[index++] = new Teacher(initial);
        }
    }

    private static void fillInTimeTable() {
        FillTable.fill37TimeTable();
        FillTable.fill38TimeTable();
        FillTable.fill39TimeTable();
    }

    // #region Calculation
    private static void calculatePoints(List<Co2Data> data, int roomNumber) {
        for (Co2Data co2Data : data) {
            Date temp = co2Data.getDate();
            int intHour = temp.getHour();
            int intMinute = temp.getMinute();
            if (FillTable.isBreak(intHour, intMinute)) {
                String whatBreak = FillTable.whatBreakIsIt(intHour, intMinute);
                int[] time = FillTable.whatTime(intHour, intMinute);

                switch (whatBreak) {
                    case "short":
                        calculateFiveMinuteBreakPoints(data, time, roomNumber);
                        break;
                    case "long":
                        calculateLongerBreakPoints(data, time, roomNumber);
                        break;

                    default:
                        break;
                }
            }
        }
    }

    private static int calculateFiveMinuteBreakPoints(List<Co2Data> data, int[] time, int roomNumber) {
        // Calculate the time range for the break (e.g., a 5-minute break)
        int startMinute = time[0];
        int endMinute = time[1];

        int points = 5; // Start with maximum points

        // Loop over each minute in the break and check the CO2 level
        for (int i = startMinute; i < endMinute; i++) {
            // Get the CO2 data for the current minute (using the data list)
            Co2Data minuteData = getCo2DataForMinute(data, i); // data is already a List<Co2Data>

            if (minuteData != null) {
                int currentCo2 = minuteData.getCo2Level();
                Co2Data previousMinuteData = getCo2DataForMinute(data, i - 1); // data is a List<Co2Data>

                if (previousMinuteData != null) {
                    int previousCo2 = previousMinuteData.getCo2Level();

                    // If CO2 level doesn't decrease, subtract points
                    if (currentCo2 >= previousCo2) {
                        points--;
                    }
                }
            }
        }

        // After calculating the points, consider any bonus points for teacher switch or
        // other criteria
        points += calculateBonusPoints(data, time, roomNumber);

        return Math.max(points, 0); // Ensure that points don't go below zero
    }

    private static int calculateLongerBreakPoints(List<Co2Data> data, int[] time, int roomNumber) {
        // Take the time for the entire break
        int startMinute = time[0];
        int endMinute = time[1];

        int points = 10; // Start with maximum points for longer break

        // Loop over each minute in the break and check the CO2 level
        for (int i = startMinute; i < endMinute; i++) {
            // Get the CO2 data for the current minute (using the data list)
            Co2Data minuteData = getCo2DataForMinute(data, i); // data is already a List<Co2Data>

            if (minuteData != null) {
                int currentCo2 = minuteData.getCo2Level();
                Co2Data previousMinuteData = getCo2DataForMinute(data, i - 1); // data is a List<Co2Data>

                if (previousMinuteData != null) {
                    int previousCo2 = previousMinuteData.getCo2Level();

                    // If CO2 level doesn't decrease, subtract points
                    if (currentCo2 >= previousCo2) {
                        points--;
                    }
                }
            }
        }

        // After calculating the points, consider any bonus points for teacher switch or
        // other criteria
        points += calculateBonusPoints(data, time, roomNumber);

        return Math.max(points, 0); // Ensure that points don't go below zero
    }

    private static int calculateBonusPoints(List<Co2Data> data, int[] time, int roomNumber) {
        // Check if the next lesson isn't lunch or the same teacher (if the next lesson
        // is lunch, no bonus points possible)
        boolean isTeacherChange = isTeacherChange(data, roomNumber); // Check if the teacher is changing
        boolean isNextLessonLunch = isNextLessonLunch(data, roomNumber);

        if (!isNextLessonLunch && isTeacherChange) {
            return 5; // Bonus points for teacher switch
        } else {
            return 0; // No bonus points
        }
    }

    private static boolean isTeacherChange(List<Co2Data> data, int roomNumber) {

        return false; // Placeholder logic
    }

    private static boolean isNextLessonLunch(List<Co2Data> data, int roomNumber) {
        // Logic to check if the next lesson is lunch (can be based on time and lesson
        // schedule)
        return false; // Placeholder logic
    }

    private static Co2Data getCo2DataForMinute(List<Co2Data> data, int minute) {
        // Logic to get the CO2 data for a specific minute in the break
        for (Co2Data co2Data : data) {
            if (co2Data.getDate().getMinute() == minute) {
                return co2Data;
            }
        }
        return null; // Return null if no data matches
    }

    // #region Sorting Printing
    private static void sortTeachers() {
        Arrays.sort(teachers,
                (a, b) -> Integer.compare(b.getPoints().getTotalPoints(), a.getPoints().getTotalPoints()));
    }

    private static void printTeachers() {
        int rank = 1;
        int previousPoints = -1;
        int currentRank = 1;

        for (int i = 0; i < teachers.length; i++) {
            Teacher teacher = teachers[i];
            int teacherPoints = teacher.getPoints().getTotalPoints();

            if (teacherPoints == previousPoints) {
                System.out.println(currentRank + ". " + teacher.getName() + " " + teacherPoints);
            } else {
                if (i > 0) {
                    rank += (i - (currentRank - 1));
                }
                currentRank = rank;
                System.out.println(rank + ". " + teacher.getName() + " " + teacherPoints);
            }

            previousPoints = teacherPoints;
        }
    }

    // #region User Interaction
    private static int getUserInput(String textOutput) {
        System.out.println(textOutput);
        while (true) {
            if (scanner.hasNextInt()) {
                return scanner.nextInt();
            }
            scanner.next(); // Clear invalid input
        }
    }

    private static void printExplanation() {
        System.out.println("Point calculation explanation:");
        System.out.println("1. Up to 5 points for keeping the window open during a small pause.");
        System.out.println("2. Up to 10 points for long pauses, depending on window usage.");
        System.out.println("3. 5 bonus points for teacher switches in classrooms.");
        System.out.println("4. Deduct points if CO2 levels are too high.");
    }

    private static void printShutDown() {
        System.out.println("Shutting down...");
        for (int i = 3; i > 0; i--) {
            System.out.print(i + "...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // @TODO: remove this before hand-in deadline
    public static void debbugingValueLists(List<Co2Data> data) {
        // Debugging co2Data
        int index = 0;
        for (Co2Data a : data) {
            System.out.println(a.toString());
            index++;
        }

        System.out.println("-----------------");
        System.out.println(index);
    }

    // #region Main
    public static void main(String[] args) {
        boolean debbugingList = false;
        if (debbugingList) {
            debbugingValueLists(ROOM_37_DATA);
            // debbugingValueLists(room38Data);
            // debbugingValueLists(room39Data);
        } else {
            System.out.println("Calculations in process please do not shut off...");
            fillInTimeTable();
            initializeTeachers();
            calculatePoints(ROOM_37_DATA, 37);
            calculatePoints(ROOM_38_DATA, 38);
            calculatePoints(ROOM_39_DATA, 39);
            sortTeachers();
            printTeachers();
            while (true) {
                int userInput = getUserInput(
                        "Do you want to see how the points were calculated? (Yes 1, No 0; anything is an error)");

                if (userInput == 1) {
                    printExplanation();
                    printShutDown();
                    break;
                } else if (userInput == 0) {
                    printShutDown();
                    break;
                } else {
                    System.out.println("Invalid input. Please enter 1 for Yes or 0 for No.");
                }
            }
            scanner.close();
        }
    }
}
