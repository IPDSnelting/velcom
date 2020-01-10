package de.aaaaaaah.designproto.runner.shared.docs;

/**
 * Contains all required criteria.
 */
public class RequiredCriteria {

	/**
	 * VelCom soll mit mehreren Repositories umgehen können und automatisch nach neuen Commits in
	 * diesen Repositories suchen.
	 */
	public static final String TRACK_MULTIPLE_REPOS = "M1";

	/**
	 * Für neue Commits soll automatisch ein Benchmark-Durchlauf ausgeführt werden. Dessen
	 * Ergebnisse sollen eingesammelt und gespeichert werden. Dabei wird dieBenchmark-Run-
	 * ner-Spezifikation verwendet.
	 */
	public static final String AUTOMATIC_BENCHMARKS = "M2";

	/**
	 * Bei einem hinzugefügten Repository muss der Repositoryadministrator angeben können, welche
	 * Branches überwacht und welche ignoriert werden sollen.
	 */
	public static final String SPECIFY_TRACKED_BRANCHES = "M3";

	/**
	 * Man muss vergleichen können, wie sich Messwerte verschiedener Repositories im Laufe der Zeit
	 * entwickelt haben.
	 */
	public static final String COMPARE_MULTIPLE_REPOS = "M4";

	/**
	 * In der Commit-Detailansicht werden die Messwerte für alle Messgrößen aller Benchmarks dieses
	 * Commits angezeigt. Zusätzlich werden alle Messwerte mit denjenigen des vorherge- henden
	 * Commits im selben Repository verglichen und Differenzen angezeigt. Wenn beim
	 * Benchmark-Durchlauf Fehler aufgetreten sind, sollen diese auch angezeigt werden.
	 */
	public static final String SHOW_BENCHMARK_RESULTS_FOR_COMMIT = "M5";

	/**
	 * Nutzer müssen eine Liste der neuesten Commits eines überwachten Repositories einsehen
	 * können.
	 */
	public static final String SHOW_LAST_RESULTS_IN_REPOSITORY = "M6";

	/**
	 * Das System darf bei Fehlschlägen von Benchmark-Durchläufen und Fehlern des Bench-
	 * mark-Runners nicht abstürzen und muss in der Commit-Detailansicht anzeigen, was
	 * schiefgelaufen ist.
	 */
	public static final String RESILIENCE = "M7";

	/**
	 * Das System unterstützt das Anzeigen von Messwerten in beliebigen Maßeinheiten.
	 */
	public static final String MEASUREMENT_UNITS = "M8";

	/**
	 * Das System priorisiert neu eintreffende Commits höher als ältere. Somit werden Bench-
	 * mark-Durchläufe für aktuelle Commits vor Benchmark-Durchläufen für ältere Commits ausgeführt
	 * und die Darstellung des Zustands über alle überwachten Repositories hinweg so aktuell wie
	 * möglich gehalten.
	 */
	public static final String PRIORITIZING_NEW_COMMITS = "M9";

	/**
	 * Um die Arbeit des Systemadministrators zu erleichtern, können manche Einstellungen auch über
	 * das Frontend erledigt werden, ohne manuell die Konfigurationsdatei zu bear- beiten. Die
	 * Rechte dafür besitzt ein Webadministrator, welcher sich mit einem Webad- ministrator-Token
	 * auf der Website authentifizieren kann. Die betroffenen Anforderungen verweisen auf dieses
	 * Kriterium.
	 */
	public static final String WEB_ADMINISTRATOR = "M10";

	/**
	 * Zwei Commits eines Repositories sollten in der Commit-Vergleichsansicht verglichen wer- den
	 * können. Unterschiedliche Messwerte werden hervorgehoben. Die Differenz der Mess- werte wird
	 * angezeigt.
	 *
	 * <p>
	 * Es werden nur die Messgrößen angezeigt, die für beide Commits existieren. Falls es keine
	 * gemeinsame Messgröße gibt, so wird auch keine angezeigt.
	 */
	public static final String COMMIT_COMPARISON = "M11";
}
