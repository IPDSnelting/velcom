package de.aaaaaaah.designproto.runner.shared.docs;

/**
 * Contains all optional criteria.
 */
public class OptionalCriteria {

	/**
	 * Die Reihenfolge von Commits in linearen Abschnitten der Commit-Historie eines Repo- sitories
	 * muss für Anwendungen wie der Commit-Detailansicht oder der Repository-De- tailansicht
	 * erhalten bleiben.
	 *
	 * <p>
	 * Lineare Abschnitte sind Bereiche im von der Commit-Historie gebildeten Graphen, in denen
	 * jeder Commit nur genau einen Vorgänger und maximal einen Nachfolger besitzt.
	 */
	public static final String PRESERVE_LINEAR_HISTORY = "K1";

	/**
	 * Farben in Graphen sollen für Personen mit und ohne Farbenblindheit gut unterscheidbar sein.
	 */
	public static final String HIGH_COLOR_CONTRAST = "K2";

	/**
	 * In der Repository-Vergleichsansicht sollen sich beim Ein- und Ausblenden von Reposito- ries
	 * die Farben, in welchen die einzelnen Repositories angezeigt werden, nicht ändern.
	 */
	public static final String CONSISTENT_COLORS = "K3";

	/**
	 * Ein Nutzer kann die Warteschlange an Commits, die vom Benchmark-Server gebench- markt werden,
	 * einsehen. Ein Webadministrator kann diese umordnen.
	 */
	public static final String VIEW_QUEUE = "K4";

	/**
	 * Ein Webadministrator kann für einen oder mehrere Commits gezielt Benchmark-Durch- läufe neu
	 * starten.
	 */
	public static final String RESTART_RUNS = "K5";

	/**
	 * Die Graphen in der Repository-Vergleichsansicht und der Repository-Detailansicht sollen eine
	 * Zoomfunktion bereitstellen, mit deren Hilfe man einen ausgewählten Bereich ver- größern kann.
	 * Sie soll per Maus bedienbar sein. Zudem soll man den Wertebereich der Graphen einschränken
	 * können, um die Anzahl der angezeigten Commits anzupassen.
	 */
	public static final String ZOOM_IN_GRAPH = "K6";

	/**
	 * Es soll möglich sein, das Untersuchen eines überwachten Repositories auf neue Commits manuell
	 * zu veranlassen.
	 */
	public static final String PUSH_EVENT_HOOKS = "K7";
	/**
	 * Permalinks sind nützlich, um beispielsweise eine bestimmte Ansicht mit einer anderen Person
	 * zu teilen oder in Dokumentation auf Ansichten zu verlinken.
	 */
	public static final String PERMANENT_LINKS = "K8";

	/**
	 * Wenn Maßeinheiten so geändert werden, dass sie kompatible Größen messen (z. B. Sekun- den und
	 * Millisekunden), werden diese automatisch auf eine gemeinsame Maßeinheit um- gerechnet, damit
	 * die verschiedenen Messwerte direkt verglichen werden können. Dies ist mindestens für die
	 * Einheiten Millisekunde, Sekunde, Minute, Stunde und für SI-Präfixe, sowie für Binärpräfixe
	 * (Ki, Mi, . . . ) möglich.
	 */
	public static final String CONVERT_MEASUREMENT_UNITS = "K9";

	/**
	 * Ähnlich zu M6 sollen neue Commits gesondert angezeigt werden, falls sie in mindestens einem
	 * Messergebnis mehr als die doppelte Standardabweichung vom Erwartungswert für diese Messgröße
	 * abweichen und deswegen als signifikant bewertet werden.
	 */
	public static final String SHOW_SIGNIFICANT_LAST_RESULTS = "K10";

	/**
	 * In der Commit-Detailansicht und, falls M11 implementiert wird, der Commit-Vergleichs-
	 * ansicht, werden zu jeder Messgröße zusätzlich die Standardabweichung für diese Mess- größe
	 * und die Signifikanz der Messwerte der Commits angezeigt.
	 */
	public static final String STATISTICS = "K11";

	/**
	 * Als Erweiterung von M11 sollen zwei Commits verschiedener Repositories verglichen werden
	 * können. Unterschiedliche Messgrößen werden hervorgehoben und die Differenz der Messgrößen
	 * wird angezeigt.
	 */
	public static final String COMPARE_COMMITS_FROM_DIFFERENT_REPOS = "K12";

	/**
	 * Es wird eine Ansicht angeboten, in der Nutzer die verschiedenen Branches eines Reposi- tories
	 * vergleichen können.
	 */
	public static final String COMPARE_BRANCHES = "K13";

	/**
	 * Dies ist eine Erweiterung von M10 . Neben dem Webadministrator soll es für jedes Repository
	 * einen Repositoryadministrator mit erweiterten Rechten für dieses Repository geben.
	 */
	public static final String REPO_ADMINS = "K14";

	/**
	 * Der Nutzer kann über ein Drop-down-Menü auswählen, ob ihm Bezeichnungen im Fron- tend in
	 * deutscher oder englischer Sprache angezeigt werden.
	 */
	public static final String LOCALIZATION = "K15";

	/**
	 * Die neuesten Commits aller Repositories sollten in der Repository-Neuigkeiten-Ansicht
	 * angezeigt werden. Sollte K11 implementiert werden, so werden die Commits dement- sprechend
	 * gekennzeichnet.
	 */
	public static final String NEWS_PAGE = "K16";

	/**
	 * Falls das Benchmark-Skript in Benchmark-Ergebnissen angibt, ob ein größerer Messwert eine
	 * Verbesserung oder eine Verschlechterung der Messgröße bedeutet, oder ob die Mess- größe
	 * diesbezüglich neutral ist, wird diese Information in der Darstellung genutzt.
	 */
	public static final String MEASUREMENT_INTERPRETATION = "K17";

	/**
	 * Anzeigeeinstellungen zu Messgrößen können auf der Webseite geändert werden.
	 */
	public static final String DEFINE_MEASUREMENT_VALUE_DISPLAY = "K18";

}
