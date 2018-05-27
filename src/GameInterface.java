import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GameInterface extends Remote {
	/**
	 * Wysokość planszy - liczba wierszy
	 */
	public final int HIGHT = 27;

	/**
	 * Szerokość planszy - liczba kolumn
	 */
	public final int WIDTH = 11;

	/**
	 * Opóźnienie bazowe do wyznaczania innych stałych.
	 */
	public final long DELAY = 330;

	/**
	 * Czas od chwili rozpoczęcia gry do pierwszego zaakceptowania wywołania metody
	 * fire(). Przed upływem tego czasu wywołania fire() będa ignorowane (nic nie
	 * będzie się działo).
	 */
	public final long THE_LULL_BEFORE_THE_STORM = DELAY * 7;

	/**
	 * Połowa okresu czasu pomiędzy strzałami
	 */
	public final long FIRE_DELAY_HALF = (long) (0.65 * DELAY);
	/**
	 * Połowa okresu czasu pomiędzy zmianami położenia okrętu
	 */
	public final long MOVE_DELAY_HALF = (long) (0.4 * DELAY);
	/**
	 * Połowa okresu czasu pomiędzy dwoma zmianami kierunku o 90 stopni
	 */
	public final long TURN_DELAY_HALF = (long) (0.45 * DELAY);

	/**
	 * Odleglość detekcji okrętu w spoczynku
	 */
	public final double SPOT_DISTANCE_IN_REST = Math.sqrt(2 * 4 * 4);

	/**
	 * Odleglość detekcji okretu bedacego w ruchu
	 */
	public final double SPOT_DISTANCE_IN_MOTION = Math.sqrt(2 * 5 * 6);

	/**
	 * Odleglość detekcji okretu, ktory strzela
	 */
	public final double SPOT_DISTANCE_SHOOTING = Math.sqrt(2 * 7 * 7);

	/**
	 * Typ wyliczeniowy dostepnych kursow okrętu
	 */
	public enum Course implements Serializable {
		NORTH {
			public Position next(Position p) {
				return new Position(p.getCol(), p.getRow() + 1);
			}

			@Override
			public Course afterTurnToLeft() {
				return WEST;
			}

			@Override
			public Course afterTurnToRight() {
				return EAST;
			}

			public String toString() {
				return "^";
			}
		},
		SOUTH {
			public Position next(Position p) {
				return new Position(p.getCol(), p.getRow() - 1);
			}

			@Override
			public Course afterTurnToLeft() {
				return EAST;
			}

			@Override
			public Course afterTurnToRight() {
				return WEST;
			}

			public String toString() {
				return "v";
			}

		},
		EAST {
			public Position next(Position p) {
				return new Position(p.getCol() + 1, p.getRow());
			}

			@Override
			public Course afterTurnToLeft() {
				return NORTH;
			}

			@Override
			public Course afterTurnToRight() {
				return SOUTH;
			}

			public String toString() {
				return ">";
			}
		},
		WEST {
			public Position next(Position p) {
				return new Position(p.getCol() - 1, p.getRow());
			}

			@Override
			public Course afterTurnToLeft() {
				return SOUTH;
			}

			@Override
			public Course afterTurnToRight() {
				return NORTH;
			}

			public String toString() {
				return "<";
			}
		};
		/**
		 * Metoda zwraca kolejne polozenie okretu poruszajacego sie danym kursem
		 * 
		 * @param p
		 *            aktualna pozycja okretu
		 * @return kolejna pozycja okretu poruszajacego sie danym kursem
		 */
		abstract public Position next(Position p);

		/**
		 * Zwraca nazwe kierunku kursu
		 * 
		 * @return nazwa kierunku
		 */
		public String fullCourseName() {
			return name();
		}

		/**
		 * Metoda zwraca kurs okrętu po wykonaniu skrętu w prawo o 90 stopni
		 * 
		 * @return kurs po skręcie w prawo o 90 stopni
		 */
		abstract public Course afterTurnToRight();

		/**
		 * Metoda zwraca kurs okrętu po wykonaniu skrętu w lewo o 90 stopni
		 * 
		 * @return kurs po skręcie w lewo o 90 stopni
		 */
		abstract public Course afterTurnToLeft();
	}

	/**
	 * Klasa do produkcji niezmienniczych obiektów reprezentujacych położenie na
	 * planszy.
	 * 
	 */
	public class Position implements Serializable {
		private static final long serialVersionUID = -5006829363311504738L;
		private final int row;
		private final int col;

		public Position(int col, int row) {
			this.row = row;
			this.col = col;
		}

		/**
		 * Numer wiersza
		 * 
		 * @return numer wiersza
		 */
		public int getRow() {
			return row;
		}

		/**
		 * Numer kolumny
		 * 
		 * @return numer kolumny
		 */
		public int getCol() {
			return col;
		}

		@Override
		public boolean equals(Object obj) {
			if (!(obj instanceof Position))
				return false;
			Position ps = (Position) obj;

			return (ps.row == row) && (ps.col == col);
		}

		@Override
		public int hashCode() {
			return 1024 * (1 + row) + col;
		}

		@Override
		public String toString() {
			return "[" + col + "," + row + "]";
		}
	}

	public class PositionAndCourse implements Serializable {
		private static final long serialVersionUID = -7756007375536743336L;
		private final Position pos;
		private final Course crs;
		private final long time;

		public PositionAndCourse(Position pos, Course crs) {
			this.pos = pos;
			this.crs = crs;
			time = System.currentTimeMillis();
		}

		/**
		 * Pozycja okrętu.
		 * 
		 * @return pozycja okrętu
		 */
		public Position getPosition() {
			return pos;
		}

		/**
		 * Kurs okrętu
		 * 
		 * @return kurs okrętu
		 */
		public Course getCourse() {
			return crs;
		}

		/**
		 * Wynik wywołania System.currentTimeMillis() w chwili wygenerowania obiektu.
		 * 
		 * @return informacja kiedy obiekt został utworzony
		 */
		public long getTime() {
			return time;
		}

	}

	/**
	 * Rejestruje gracza i przekazuje mu unikalny, losowy numer identyfikacyjny.
	 * 
	 * @param playerName
	 *            nazwa gracza
	 * @return numer identyfikacyjny gracza
	 */
	public long register(String playerName) throws RemoteException;

	/**
	 * Wstrzymuje wątek gracza do chwili dołączenia do gry przeciwnika i rozpoczęcia
	 * rozgrywki.
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 */
	public void waitForStart(long playerID) throws RemoteException;

	/**
	 * Zwraca liczbę dostepnych dla gracza okretów.
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 * @return liczba dostepnych jednostek
	 * @throws RemoteException
	 */
	public int getNumberOfAvaiablewarships(long playerID) throws RemoteException;

	/**
	 * Zwraca kurs okrętu.
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 * @param warshipID
	 *            numer jednostki
	 * @return kurs okrętu o podanym warshipID
	 * @throws RemoteException
	 *             - polecenie nie moglo zostac zrealizowane np. ze wzgledu na
	 *             zatopienie jednostki
	 */
	public Course getCourse(long playerID, int warshipID) throws RemoteException;

	/**
	 * Zwraca położenie jednostki na planszy
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 * @param warshipID
	 *            numer jednostki
	 * @return miejsce, w ktorym znajduje sie na planszy jednostka
	 * @throws RemoteException
	 *             - polecenie nie moglo zostac zrealizowane np. ze wzgledu na
	 *             zatopienie jednostki
	 */
	public Position getPosition(long playerID, int warshipID) throws RemoteException;

	/**
	 * Pozwala sprawdzić, czy jednostka o danym numerze jest nadal sprawna.
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 * @param warshipID
	 *            numer jednostki
	 * @return true - okret sprawny, false - wrak
	 * @throws RemoteException
	 *             - polecenie nie mogło zostać zrealizowane np. z powodu błędu
	 *             playerID czy warshipID.
	 */
	public boolean isAlive(long playerID, int warshipID) throws RemoteException;

	/**
	 * Zmiana kursu o 90 stopni w lewo. <br>
	 * Metoda może być wywoływana współbieżnie dla różnych warshipID. Współbieżne
	 * wywołanie z tym samym warshipID jest zabronione. <br>
	 * Czas wykonania metody to około 2xTURN_DELAY_HALF. <br>
	 * Metody turnLeft, turnRight, fire i move wywołane współbieżnie z tym samym
	 * warshipID zadziałają sekwencyjnie w nieznanej kolejności.
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 * @param warshipID
	 *            numer jednostki
	 * @throws RemoteException
	 *             - polecenie nie moglo zostac zrealizowane np. ze wzgledu na
	 *             zatopienie jednostki lub błędnych wartości identyfikatorów.
	 */
	public void turnLeft(long playerID, int warshipID) throws RemoteException;

	/**
	 * Zmiana kursu o 90 stopni w prawo.<br>
	 * Metoda może być wywoływana współbieżnie dla różnych warshipID. Współbieżne
	 * wywołanie z tym samym warshipID jest zabronione. <br>
	 * Czas wykonania metody to około 2xTURN_DELAY_HALF. <br>
	 * Metody turnLeft, turnRight, fire i move wywołane współbieżnie z tym samym
	 * warshipID zadziałają sekwencyjnie w nieznanej kolejności.
	 * 
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 * @param warshipID
	 *            numer jednostki
	 * @throws RemoteException
	 *             - polecenie nie moglo zostac zrealizowane np. ze wzgledu na
	 *             zatopienie jednostki
	 */
	public void turnRight(long playerID, int warshipID) throws RemoteException;

	/**
	 * Zmiana polozenia jednostki o jedno pole. Okręt porusza się zgodnie z
	 * aktualnym kursem.<br>
	 * Metoda może być wywoływana współbieżnie dla różnych warshipID. Współbieżne
	 * wywołanie z tym samym warshipID jest zabronione. <br>
	 * Czas wykonania metody to około 2xMOVE_DELAY_HALF. <br>
	 * Metody turnLeft, turnRight, fire i move wywołane współbieżnie z tym samym
	 * warshipID zadziałają sekwencyjnie w nieznanej kolejności.
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 * @param warshipID
	 *            numer jednostki
	 * @throws RemoteException
	 *             - polecenie nie moglo zostac zrealizowane np. ze wzgledu na
	 *             zatopienie jednostki
	 */
	public void move(long playerID, int warshipID) throws RemoteException;

	/**
	 * Oddanie strzalu na wskazaną pozycję. <br>
	 * Metoda może być wywoływana współbieżnie dla różnych warshipID. Współbieżne
	 * wywołanie z tym samym warshipID jest zabronione. <br>
	 * Czas wykonania metody to około 2xFIRE_DELAY_HALF. <br>
	 * Metody turnLeft, turnRight, fire i move wywołane współbieżnie z tym samym
	 * warshipID zadziałają sekwencyjnie w nieznanej kolejności.
	 * 
	 * <br>
	 * UWAGA: Przez okres czasu THE_LULL_BEFORE_THE_STORM od chwili rozpoczęcia gry,
	 * wywołania metody fire nie będą skutkować oddaniem strzału. Metoda będzie
	 * natychmiast kończyć pracę.
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 * @param warshipID
	 *            numer identifikacyjny jednostki, która oddaje strzał
	 * @param target
	 *            pozycja celu
	 * @return true - jednostka przeciwnika trafiona, false - pudlo
	 * @throws RemoteException
	 *             - polecenie nie moglo zostac zrealizowane np. ze wzgledu na
	 *             zatopienie jednostki, która miała oddać strzał
	 */
	public boolean fire(long playerID, int warshipID, Position target) throws RemoteException;

	/**
	 * Metoda zwraca położenie i kurs wykrytego okretu przeciwnika. Blokuje wątek,
	 * który ją wywoła do chwili pojawienia się informacji, która może zostać
	 * zwrócona użytkownikowi. Nieodebrane wiadomosci sa kolejkowane przez serwis.
	 * 
	 * @param playerID
	 *            numer identyfikacyjny gracza
	 * @return poloznie i kurs wykrytej jednostki przeciwnika
	 * @throws RemoteException
	 *             - polecenie nie mogło zostać zrealizowane np. z powodu błędnego
	 *             playerID.
	 */
	public PositionAndCourse getMessage(long playerID) throws RemoteException;

}
