import java.util.*;
import java.util.stream.*;

/**
 * Smart Attendance & Performance Tracker
 * Advanced Java Mini Project - Stream API
 *
 * Stream concepts used: filter(), mapToInt(), average(), groupingBy(), sorted()
 * (plus a few helpers: collectingAndThen, maxBy, averagingDouble, IntStream)
 *
 * Run:  java SmartAttendanceTracker.java
 *  or:  javac SmartAttendanceTracker.java && java SmartAttendanceTracker
 */

// ---------------------------------------------------------------- MAIN
public class SmartAttendanceTracker {

    static List<Student> sampleData() {
        return List.of(
            new Student(101, "Aarav",   "CSE",  List.of(92, 90, 94, 91), 88, 92, 85, 90, 87),
            new Student(102, "Divya",   "CSE",  List.of(70, 68, 72, 65), 55, 60, 48, 62, 58),
            new Student(103, "Karthik", "CSE",  List.of(95, 40, 90, 85), 76, 70, 82, 68, 74),
            new Student(104, "Meena",   "ECE",  List.of(88, 85, 90, 87), 91, 89, 94, 90, 93),
            new Student(105, "Rohan",   "ECE",  List.of(60, 55, 62, 58), 42, 38, 45, 50, 40),
            new Student(106, "Sana",    "ECE",  List.of(80, 78, 82, 79), 72, 68, 75, 70, 66),
            new Student(107, "Vikram",  "MECH", List.of(85, 88, 84, 90), 65, 70, 62, 68, 71),
            new Student(108, "Priya",   "MECH", List.of(98, 50, 95, 45), 80, 78, 85, 82, 79),
            new Student(109, "Naveen",  "MECH", List.of(72, 70, 68, 74), 58, 52, 61, 49, 55),
            new Student(110, "Lakshmi", "AI&DS",List.of(96, 94, 97, 95), 95, 92, 97, 94, 96),
            new Student(111, "Arjun",   "AI&DS",List.of(78, 82, 80, 76), 84, 79, 88, 81, 83),
            new Student(112, "Isha",    "AI&DS",List.of(90, 92, 55, 88), 69, 73, 66, 71, 74)
        );
    }

    public static void main(String[] args) {
        List<Student> students = sampleData();
        AnalyticsService service = new AnalyticsService(students);
        ReportGenerator report = new ReportGenerator(students, service);

        report.dashboard();
        report.alerts();
        report.studentReports();
    }
}

// ---------------------------------------------------------------- MODEL
class Student {
    private final int id;
    private final String name;
    private final String department;
    private final List<Integer> weeklyAttendance; // % attendance for each week
    private final int[] marks;                    // marks in each subject (out of 100)

    Student(int id, String name, String department, List<Integer> weeklyAttendance, int... marks) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.weeklyAttendance = weeklyAttendance;
        this.marks = marks;
    }

    public int getId()            { return id; }
    public String getName()       { return name; }
    public String getDepartment() { return department; }
    public List<Integer> getWeeklyAttendance() { return weeklyAttendance; }

    /** Overall attendance % = average of weekly attendance (mapToInt + average). */
    public double getAttendance() {
        return weeklyAttendance.stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public int getTotalMarks() { return Arrays.stream(marks).sum(); }

    public double getAverageMarks() { return Arrays.stream(marks).average().orElse(0); }

    public String getGrade() {
        double m = getAverageMarks();
        if (m >= 90) return "O";
        if (m >= 80) return "A+";
        if (m >= 70) return "A";
        if (m >= 60) return "B";
        if (m >= 50) return "C";
        return "F";
    }

    @Override public String toString() {
        return String.format("%-4d %-10s %-5s Att: %5.1f%%  Avg: %5.1f", id, name, department, getAttendance(), getAverageMarks());
    }
}

// ---------------------------------------------------------------- ANALYTICS ENGINE
class AnalyticsService {
    static final double ATTENDANCE_THRESHOLD = 75.0;   // minimum required %
    static final int    DROP_LIMIT           = 30;     // sudden weekly drop (points)
    static final int    SWING_LIMIT          = 40;     // max-min gap (points)

    private final List<Student> students;

    AnalyticsService(List<Student> students) { this.students = students; }

    /** 1. Filter low-attendance students, worst first. */
    List<Student> lowAttendanceStudents() {
        return students.stream()
                .filter(s -> s.getAttendance() < ATTENDANCE_THRESHOLD)
                .sorted(Comparator.comparingDouble(Student::getAttendance))
                .collect(Collectors.toList());
    }

    /** 2. Class average of total marks. */
    double classAverageMarks() {
        return students.stream().mapToInt(Student::getTotalMarks).average().orElse(0);
    }

    double classAverageAttendance() {
        return students.stream().mapToDouble(Student::getAttendance).average().orElse(0);
    }

    /** 3. Topper of each department (groupingBy + maxBy). */
    Map<String, Student> toppersByDepartment() {
        return students.stream().collect(Collectors.groupingBy(
                Student::getDepartment,
                TreeMap::new,
                Collectors.collectingAndThen(
                        Collectors.maxBy(Comparator.comparingInt(Student::getTotalMarks)),
                        Optional::get)));
    }

    /** 4. Irregular pattern = a sudden drop between weeks OR a huge swing overall. */
    boolean isIrregular(Student s) {
        List<Integer> w = s.getWeeklyAttendance();
        boolean suddenDrop = IntStream.range(1, w.size())
                .anyMatch(i -> w.get(i - 1) - w.get(i) >= DROP_LIMIT);
        int max = w.stream().mapToInt(Integer::intValue).max().orElse(0);
        int min = w.stream().mapToInt(Integer::intValue).min().orElse(0);
        return suddenDrop || (max - min) >= SWING_LIMIT;
    }

    List<Student> irregularStudents() {
        return students.stream()
                .filter(this::isIrregular)
                .sorted(Comparator.comparing(Student::getName))
                .collect(Collectors.toList());
    }

    /** 5. Department performance ranking (average marks, highest first). */
    List<Map.Entry<String, Double>> departmentRanking() {
        return students.stream()
                .collect(Collectors.groupingBy(Student::getDepartment,
                        Collectors.averagingDouble(Student::getAverageMarks)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
    }

    Map<String, Long> gradeDistribution() {
        return students.stream().collect(Collectors.groupingBy(
                Student::getGrade, TreeMap::new, Collectors.counting()));
    }

    /** Top N students overall. */
    List<Student> topN(int n) {
        return students.stream()
                .sorted(Comparator.comparingInt(Student::getTotalMarks).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    /** Remark used in student report cards. */
    String remark(Student s) {
        boolean lowAtt = s.getAttendance() < ATTENDANCE_THRESHOLD;
        boolean lowMarks = s.getAverageMarks() < 50;
        if (lowAtt && lowMarks) return "AT RISK: improve attendance and academics";
        if (lowAtt)             return "Attendance shortage - risk of detention";
        if (lowMarks)           return "Needs academic support";
        if (isIrregular(s))     return "Good, but attendance is inconsistent";
        return "Satisfactory / Keep it up";
    }
}

// ---------------------------------------------------------------- REPORTING / DASHBOARD
class ReportGenerator {
    private final AnalyticsService svc;
    private final List<Student> students;

    ReportGenerator(List<Student> students, AnalyticsService svc) {
        this.students = students;
        this.svc = svc;
    }

    private static void line(char c) { System.out.println(String.valueOf(c).repeat(66)); }

    private static String bar(double pct) {
        int filled = (int) Math.round(pct / 5);        // 20-char bar
        return "[" + "#".repeat(filled) + ".".repeat(20 - filled) + "]";
    }

    void dashboard() {
        line('=');
        System.out.println("        ACADEMIC ANALYTICS DASHBOARD");
        line('=');
        System.out.printf("Total students        : %d%n", students.size());
        System.out.printf("Class average (marks) : %.2f%n", svc.classAverageMarks());
        System.out.printf("Class avg attendance  : %.2f%%%n", svc.classAverageAttendance());
        System.out.printf("Low attendance count  : %d%n", svc.lowAttendanceStudents().size());
        System.out.printf("Irregular pattern     : %d%n", svc.irregularStudents().size());

        System.out.println("\n-- Grade distribution --");
        svc.gradeDistribution().forEach((g, c) ->
                System.out.printf("  %-3s %s %d%n", g, "*".repeat(c.intValue()), c));

        System.out.println("\n-- Department toppers --");
        svc.toppersByDepartment().forEach((d, s) ->
                System.out.printf("  %-5s -> %-10s (total %d, avg %.1f)%n",
                        d, s.getName(), s.getTotalMarks(), s.getAverageMarks()));

        System.out.println("\n-- Department performance ranking --");
        int[] rank = {1};
        svc.departmentRanking().forEach(e ->
                System.out.printf("  #%d  %-5s %6.2f %s%n", rank[0]++, e.getKey(), e.getValue(), bar(e.getValue())));

        System.out.println("\n-- Top 3 students overall --");
        svc.topN(3).forEach(s -> System.out.println("  " + s));
        System.out.println();
    }

    void alerts() {
        line('=');
        System.out.println("        ATTENDANCE ALERTS");
        line('=');
        List<Student> low = svc.lowAttendanceStudents();
        if (low.isEmpty()) System.out.println("No low-attendance students.");
        low.forEach(s -> System.out.printf("[ALERT] %-10s (%s) attendance %.1f%% < %.0f%%  -> notify mentor & parents%n",
                s.getName(), s.getDepartment(), s.getAttendance(), AnalyticsService.ATTENDANCE_THRESHOLD));

        System.out.println();
        svc.irregularStudents().forEach(s -> System.out.printf("[WARN ] %-10s irregular pattern: %s%n",
                s.getName(), s.getWeeklyAttendance()));
        System.out.println();
    }

    void studentReports() {
        line('=');
        System.out.println("        STUDENT PERFORMANCE REPORTS");
        line('=');
        students.stream()
                .sorted(Comparator.comparing(Student::getDepartment)
                        .thenComparing(Student::getTotalMarks, Comparator.reverseOrder()))
                .forEach(s -> {
                    System.out.printf("ID %d | %s | Dept %s%n", s.getId(), s.getName(), s.getDepartment());
                    System.out.printf("   Attendance : %.1f%% %s%n", s.getAttendance(), bar(s.getAttendance()));
                    System.out.printf("   Marks      : total %d, avg %.1f, grade %s%n",
                            s.getTotalMarks(), s.getAverageMarks(), s.getGrade());
                    System.out.printf("   Remark     : %s%n", svc.remark(s));
                    line('-');
                });
    }
}
