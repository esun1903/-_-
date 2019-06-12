import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import Dependencies.DeepCopy;

// WARNING:성능, 가지치기 및 노드 선택에 최적화되지 않음!
public class Jack {
	private static final int SUFFICIENTLY_LARGE_NUMBER = 100_000_000;
	private static final int WIN_NUMBER = 256;
	private static final int DEPTH_LIMIT = 7; //실제 깊이는 한계 + 1
	private static final int BRANCH_LIMIT = 5;
	private double DEFENSE_WEIGHT = 0.92;// 방어보다 공격의 우선 순위를 매기는 것을 장려하다. 숫자가 커질수록 방어률 높아짐
	private static final double THRESHOLD = 2/3;                
	private double[] time;
	private int turn = 1, nodes; //턴: -1은 흰색, 1은 검은색. 카운트는 첫 번째 이동에만 사용됨
	private int[][] board, scores; // actual board for storing pieces. Separate from storing board space scores
	private Map<Point, List<List<PI>>> threatSpaces; // 위협 -> 위협 공간 선 -> 공간 & 점수
	private Map<Point, List<List<Point>>> lookup; // threat space (incl. 0) -> list of threat sequences
	// TODO: figure out why AI sometimes ignores best moves when it's white, even though it's smart when it goes first
	// TODO: make AI non-retarded - it even manages to ignore straight rows of 4 (when score is 100000000)
	// TODO: lazy parallelization - using the fact that there are at max 4 or 5 on the first level, streamify the first level and parallelize it
	// TODO: implement undo using deep copy
	// TODO: obvious optimization - remove "dead branches" in both threatspaces and lookup
	// TODO: optimization -  prioritize only the directly neighboring spaces when there's immediate threat
	// TODO: optimization - should make threat detection much less lengthy (don't go over entire lookup again)
	// TODO: optimization - just ignore scores & spaces that are insignificant, in both alternating and tallying up

	// custom data type
	public void setDefense(double input) {
		this.DEFENSE_WEIGHT = input;
	}
	public double getDefense() {
		return this.DEFENSE_WEIGHT;
	}
	public class PI {
		private Point p;
		private int i;

		public PI (Point p, int i) {
			this.p = p; this.i =i;
		}

		public Point getP() {
			return p;
		}  

		public int getI() {
			return i;
		}

		public void setI(int i) {
			this.i = i;
		}

		public String toString() {
			return "<("+p.x+", "+p.y+"), "+i+">";
		}
	}

	// constructor
	public Jack() {
		board = new int[19][19];
		threatSpaces = new HashMap<>();
		lookup = new HashMap<>();
		scores = new int[15][15];
		time = new double[2];
	}

	// 공식적으로 포인트를 추가하여 실제 위협 영역 및 조회 수정
	public void addPoint(int x, int y) {
		board[x][y] = turn;
		turn = 0 - turn;
		// 조회 및 위협 영역에 점 추가
		threatSpaces = step(x, y, threatSpaces, lookup, turn, board);
		lookup = hash(lookup, x, y, board, turn);
		scores = calculateScores(lookup, threatSpaces, board, turn);
	}

	// 새로운 포인트를 부여한  배열, 위협 공간 및 점수 수정
	private Map<Point, List<List<PI>>> step(int x, int y, Map<Point, List<List<PI>>> threatSpaces, Map<Point,
			List<List<Point>>> lookup, int turn, int[][] board) {
		Map<Point, List<List<PI>>> result = new HashMap<>();
		// 첫째, 영향을 받고 영향을 받지 않는 점수로 대체 점수는 대체 점수로 대체될 필요가 있다.
		for (Point threat : threatSpaces.keySet()) {
			List<List<PI>> updatedList = new ArrayList<>();
			for (List<PI> threatLine : threatSpaces.get(threat)) {
				//List<PI> newList = new ArrayList<>(threatLine);
				List<PI> newList = copyList(threatLine);
				//만약 순서의 색이 방금 돌을 내려놓은 사람의 색깔과 같다면
				if (board[threat.x][threat.y] == turn) {
					for (PI spaceScore : newList) {
						// multiply all scores by 2
						spaceScore.setI(spaceScore.getI() * 2);
					}
				} else {
					for (PI spaceScore : newList) {
						spaceScore.setI(spaceScore.getI() / 2);
					}
				}
				updatedList.add(newList);
			}
			//위협을 복사하다????
			Point newThreat = new Point(threat.x, threat.y);
			result.put(newThreat, updatedList);
		}
		// 포인트를 찾아 그 포인트로 인해 어떤 영향이 생기는지 알아보기  
		Point latestPoint = new Point(x, y); // the point that is affecting  최신 포인트 변수 만들기 (이걸로 계산하는 것 같음)
		if (lookup.containsKey(latestPoint)) {
			for (List<Point> threatSequence : lookup.get(latestPoint)) {
				for (Point threat : threatSequence) {  //point threat에 threeatSequence를 하나씩 다 대입해보기 
					if (board[threat.x][threat.y] != turn) { // same color
						// 먼저, 영향을 받는 위협 공간의 점수를 업데이트하십시오.
						int[] temp = threatSpaceFinder(threat, latestPoint);
						try {
							result.get(threat).get(temp[0]).get(temp[1]).setI(0);
						} catch (Exception e) {
							System.out.println("Error in step. Threat: "+threat.toString()+", access: "+temp[0]+
									", "+temp[1]+", latest point: ("+x+","+y+")");
						}
					} else {
						// TODO:  5을 위해 컬러-점검 다른 쪽 반대하여 그때 그에 따라 위협 공간을 제거한다.
						boolean blocked = false;
						for (int i=0; i<8; i++) {
							for (int j=0; j<result.get(threat).get(i).size(); j++) {
								if (!blocked && result.get(threat).get(i).get(j).getP().equals(latestPoint)) {
									List<PI> newLine = new ArrayList<>();
									for (int k=0; k<j; k++) {
										newLine.add(result.get(threat).get(i).get(k));
									}
									if (j > 0) { // reduce score of the stone adjacent to the opposing piece by 1/4th
										newLine.set(j-1, new PI(newLine.get(j-1).getP(), newLine.get(j-1).getI() / 4));
									} else if (j == 0) { // reduce score of other side by 1/4
										int opposite = (i + 4) % 8;
										for (int k=0; k<result.get(threat).get(opposite).size(); k++) {
											result.get(threat).get(opposite).get(k).setI(result.get(threat)
													.get(opposite).get(k).getI() / 4);
										}
									}
									result.get(threat).set(i, newLine);
									blocked = true;
								}
							}
						}
					}
				}
			}
		}
		// this is a new threat 'sequence' containing only one point (goes 8-way)
		// insert the threat point - expand until board boundary or opposite color is reached. when same color, score 0
		int[] xFactor = {1, 1}, yFactor = {0, 1};
		List<List<PI>> threatLines = new ArrayList<>();
		List<Integer> blocked = new ArrayList<>();
		for (int i=0; i<4; i++) {
			for (int j=0; j<=1; j++) {
				List<PI> threatLine = new ArrayList<>();
				boolean clash = false;
				for (int k=1; k<=5; k++) {
					if (!clash) {
						int xt = x + k * xFactor[j];
						int yt = y + k * yFactor[j];
						if (0<=xt && xt<19 && 0<=yt && yt<19 && board[xt][yt] != turn) {
							if (board[xt][yt] == 0) {
								if (k != 5) { // actual threat space
									threatLine.add(new PI(new Point(xt, yt), -4 * turn)); // starting value
								} else { // 0-space
									threatLine.add(new PI(new Point(xt, yt), 0));
								}
							} else { // same color
								threatLine.add(new PI(new Point(xt, yt), 0));
							}
						} else {
							if (!threatLine.isEmpty()) { // out of bounds or differing color
								threatLine.get(k - 2).setI(threatLine.get(k - 2).getI() / 4);
							}
							if (k == 1) {
								blocked.add(2 * i + j);
							}
							clash = true;
						}
					}
				}
					/*if (!threatLine.isEmpty())*/ threatLines.add(threatLine);
			}
			// rotate 90째 left
			int[] temp = Arrays.copyOf(xFactor,2);
			for (int j=0; j<=1; j++) {
				xFactor[j] = 0 - yFactor[j];
				yFactor[j] = temp[j];
			}
		}
		result.put(latestPoint, threatLines);
		// adjusting score of threat spaces of opposite side of wherever an opposing piece is directly touching
		if (!blocked.isEmpty()) {
			for (int side : blocked) {
				int oppositeSide = (side + 4) % 8;
				for (int i=0; i<result.get(latestPoint).get(oppositeSide).size(); i++) {
					result.get(latestPoint).get(oppositeSide).get(i)
							.setI(result.get(latestPoint).get(oppositeSide).get(i).getI() / 4);
				}
			}
		}
		return result;
	}

	private int[] threatSpaceFinder(Point threat, Point threatSpace) {
		int[] result = new int[2];
		if (threat.x > threatSpace.x) {
			// to the left
			if (threat.y > threatSpace.y) {
				// NW
				result[0] = 5;
			} else if (threat.y < threatSpace.y) {
				// SW
				result[0] = 3;
			} else {
				// left
				result[0] = 4;
			}
			result[1] = threat.x - threatSpace.x - 1;
		} else if (threat.x < threatSpace.x) {
			// to the right
			if (threat.y > threatSpace.y) {
				// NE
				result[0] = 7;
			} else if (threat.y < threatSpace.y) {
				// SE
				result[0] = 1;
			} else {
				// right
				result[0] = 0;
			}
			result[1] = threatSpace.x - threat.x - 1;
		} else {
			if (threat.y > threatSpace.y) {
				// up top
				result[0] = 6;
			} else {
				// down low
				result[0] = 2;
			}
			result[1] = Math.abs(threatSpace.y - threat.y) - 1;
		}
		return result;
	}

	// keeps track of all the sequences for each threat space
	private Map<Point, List<List<Point>>> hash(Map<Point, List<List<Point>>> lookup, int x, int y, int[][] board,
											   int turn) {
		Map<Point, List<List<Point>>> result = (Map) DeepCopy.copy(lookup);
		Point latestPoint = new Point(x, y);
		if (result.containsKey(latestPoint)) result.remove(latestPoint);
		int[] xFactor = {1, 1}, yFactor = {0, 1};
		for (int i=0; i<4; i++) {
			for (int j=0; j<2; j++) {
				boolean blocking = false;
				for (int k = 1; k<5; k++) {
					// check all threat spaces that could be affected by the latest point
					int xt = x + k * xFactor[j];
					int yt = y + k * yFactor[j];
					if (0<=xt && xt<19 && 0<=yt && yt<19) {
						if (board[xt][yt] == 0) {
							// a valid threat space
							Point threatSpace = new Point(xt, yt);
							boolean exists = false, found = false, toAdd = false;
							if (result.containsKey(threatSpace)) {
								// modify existing sequence that has this threat space
								for (List<Point> seq : lookup.get(threatSpace)) {
									int index = result.get(threatSpace).indexOf(seq);
									if (index != -1) {
										List<Point> sequence = result.get(threatSpace).get(index);
										// check if the sequence is valid
										if (inRange(latestPoint, sequence.get(0)) ||
												inRange(latestPoint, sequence.get(sequence.size() - 1))) {
											exists = true;
											// and check if the sequence, threat space, and the new threat all line up
											if (inLine(sequence.get(0), threatSpace, latestPoint)) {
												found = true;
												if (board[sequence.get(0).x][sequence.get(0).y] != turn &&
														isClear(sequence.get(sequence.size() - 1), latestPoint, turn, board)) {
													if (sequence.size() == 1) {
														if (closer(threatSpace, sequence.get(0), x, y)) {
															sequence.add(0, latestPoint);
														} else {
															sequence.add(latestPoint);
														}
													} else {
														boolean out = false;
														for (Point p : sequence) {
															if (!inRange(latestPoint, p)) {
																out = true;
															}
														}
														if (!out) {
															sequence.add(position(sequence, latestPoint, length(sequence)),
																	latestPoint);
														} else {
															List<Point> temp = splitOff(latestPoint, sequence);
															result.get(threatSpace).add(temp);
														}
													}
												} else {
													// the sequence has different color from the new threat point, so
													// trim the existing sequence as necessary
													List<Point> opposite = oppositeSide(latestPoint, threatSpace, sequence);
													if (!opposite.isEmpty()) {
														for (Point p : opposite) {
															sequence.remove(p);
														}
														boolean flag = false;
														if (!sequence.isEmpty()) {
															for (List<Point> branch : result.get(threatSpace)) {
																if (!branch.equals(sequence) &&
																		board[branch.get(0).x][branch.get(0).y] == turn &&
																		inLine(branch.get(0), threatSpace, sequence.get(0))) {
																	for (Point p : sequence) {
																		if (!branch.contains(p)) flag = true;
																	}
																	if (!flag) {
																		result.get(threatSpace).remove(sequence);
																		break;
																	}
																}
															}
														} else {
															sequence.add(latestPoint);
														}
													} else if (!blocking) {
														toAdd = true;
													}
												}
											}
										}
									}
								}
							}
							// threat space doesn't exist or none of the sequences affect latest point
							if (!blocking && (!exists || !found || toAdd)) {
								boolean alreadyIn = false;
								if (result.containsKey(threatSpace)) {
									for (List<Point> branch : result.get(threatSpace)) {
										if (branch.contains(latestPoint)) {
											alreadyIn = true;
											break;
										}
									}
								}
								if (!alreadyIn) {
									// add in new sequence
									List<Point> sequence = new ArrayList<>();
									sequence.add(latestPoint);
									if (!exists) {
										List<List<Point>> sequenceList = new ArrayList<>();
										sequenceList.add(sequence);
										result.put(threatSpace, sequenceList);
									} else {
										result.get(threatSpace).add(sequence);
									}
								}
							}
						} else if (board[xt][yt] == turn) {
							blocking = true;
						}
					}
				}
			}
			// rotate 90째 left
			int[] temp = Arrays.copyOf(xFactor,2);
			for (int j=0; j<=1; j++) {
				xFactor[j] = 0 - yFactor[j];
				yFactor[j] = temp[j];
			}
		}
		return result;
	}

	// returns true if new point is closer than existing point to threat space
	private boolean closer(Point origin, Point existing, int x, int y) {
		return (origin.x - x) * (origin.x - x) + (origin.y - y) * (origin.y - y) <
				(origin.x - existing.x) * (origin.x - existing.x) + (origin.y - existing.y) * (origin.y - existing.y);
	}

	// if three points are in line, returns true
	private boolean inLine(Point A, Point B, Point C) {
		return A.x * (B.y - C.y) + B.x * (C.y - A.y) + C.x * (A.y - B.y) == 0;
	}

	// returns true if a point is within range of 4
	private boolean inRange(Point o, Point toCheck) {
		return toCheck.x >= o.x - 4 && toCheck.x <= o.x + 4 && toCheck.y >= o.y - 4 && toCheck.y <= o.y + 4;
	}

	// returns relative position of latest point in relation to existing sequence
	private int position(List<Point> sequence, Point latestPoint, int length) {
		// should now directly return the number which should be put in the sequence
		Point start = sequence.get(0), end = sequence.get(sequence.size() - 1);
		int xt = (end.x - start.x) / length, yt = (end.y - start.y) / length;
		if (xt != 0) {
			int absPosition = (latestPoint.x - start.x) / xt;
			if (absPosition < 0) {
				return 0;
			} else if (absPosition > length) {
				return sequence.size();
			} else {
				int relPosition = 1;
				while (absPosition > (sequence.get(relPosition).x - start.x) / xt) {
					relPosition++;
				}
				return relPosition;
			}
		} else {
			// only deal with yt
			int absPosition = (latestPoint.y - start.y) / yt;
			if (absPosition < 0) {
				return 0;
			} else if (absPosition > length) {
				return sequence.size();
			} else {
				int relPosition = 1;
				while (absPosition > (sequence.get(relPosition).y - start.y) / yt) {
					relPosition++;
				}
				return relPosition;
			}
		}
	}

	// checks if there are no opposing colors between the end point and the new point
	private boolean isClear(Point end, Point latestPoint, int turn, int[][] board) {
		if (end.x == latestPoint.x) {
			// vertical
			for (int i = 1; i < Math.abs(end.y - latestPoint.y); i++) {
				if (board[end.x][Math.min(end.y, latestPoint.y) + i] == turn) return false;
			}
		} else if (end.y == latestPoint.y) {
			// horizontal
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][end.y] == turn) return false;
			}
		} else if ((end.y - latestPoint.y)/(end.x - latestPoint.x) == 1){
			// right up diagonal (slope = 1)
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][Math.min(end.y, latestPoint.y) + i] == turn) return false;
			}
		} else {
			// right down diagonal (slope = -1)
			for (int i = 1; i < Math.abs(end.x - latestPoint.x); i++) {
				if (board[Math.min(end.x, latestPoint.x) + i][Math.max(end.y, latestPoint.y) - i] == turn) return false;
			}
		}
		return true;
	}

	// given a sequence in which at least one element is out of range and a point, splits off a new sequence
	private List<Point> splitOff(Point latestPoint, List<Point> sequence) {
		List<Point> result = new ArrayList<>();
		result.add(latestPoint);
		Map<Integer, Point> distance = new HashMap<>();
		for (Point p : sequence) {
			distance.put((p.x - latestPoint.x) * (p.x - latestPoint.x) +
					(p.y - latestPoint.y) * (p.y - latestPoint.y), p);
		}
		for (int i=1; i<5; i++) {
			for (Integer d : distance.keySet()) {
				if (d == i * i || d == i * i * 2) {
					result.add(distance.get(d));
				}
			}
		}
		return result;
	}

	// lists all points in the sequence that are on the opposite side of threat space, with latest point as axis
	// also, can assume that the sequence and point are in line
	private List<Point> oppositeSide(Point latestPoint, Point threatSpace, List<Point> sequence) {
		List<Point> result = new ArrayList<>();
		if (latestPoint.x == threatSpace.x) {
			// vertical
			if (latestPoint.y > threatSpace.y) {
				for (Point p : sequence) {
					if (p.y > latestPoint.y) result.add(p);
				}
			} else {
				for (Point p : sequence) {
					if (p.y < latestPoint.y) result.add(p);
				}
			}
		} else if (latestPoint.x > threatSpace.x) {
			for (Point p : sequence) {
				if (p.x > latestPoint.x) result.add(p);
			}
		} else {
			for (Point p : sequence) {
				if (p.x < latestPoint.x) result.add(p);
			}
		}
		return result;
	}

	private int[][] calculateScores(Map<Point, List<List<Point>>> lookup, Map<Point, List<List<PI>>> threatSpaces,
									int[][] board, int turn) {
		int[][] result = new int[19][19];
		for (Point threatSpace : lookup.keySet()) {
			int black = 0, white = 0;
			// sequence-based scoring
			for (List<Point> threatSequence : lookup.get(threatSpace)) {
				int seqScore = 0, blackCount = 0, whiteCount = 0;
				// for points within a sequence, multiply them
				for (Point threat : threatSequence) {
					int[] temp = threatSpaceFinder(threat, threatSpace);
					try { // for bug fixing
						if (board[threatSequence.get(0).x][threatSequence.get(0).y] == 1) {
							if (blackCount == 0) {
								seqScore = threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
							} else {
								if (turn == 1) {
									seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() / 2);
								} else {
									seqScore *= threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
								}
							}
							blackCount++;
						} else {
							if (whiteCount == 0) {
								seqScore = threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI();
							} else {
								if (turn == 1) {
									seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() * -1);
								} else {
									seqScore *= (threatSpaces.get(threat).get(temp[0]).get(temp[1]).getI() / -2);
								}
							}
							whiteCount++;
						}
					} catch (Exception e) {
						System.out.println("Error in calc. Threat: "+threat.toString()+", threatSpace: "+threatSpace.toString());
						List<PI> log = new ArrayList<>();
						for (Point p : threatSpaces.keySet()) {
							if (!this.threatSpaces.containsKey(p)) log.add(new PI(p, 0));
						}
						for (PI pi : log) {
							pi.setI(board[pi.getP().x][pi.getP().y]);
						}
						System.out.println("Log: "+log.toString());
					}
				}
				// then add up the scores of the sequences
				if (board[threatSequence.get(0).x][threatSequence.get(0).y] == 1) {
					if (blackCount == 4) { // winning condition
						black = SUFFICIENTLY_LARGE_NUMBER;
					} else {
						black += seqScore;
					}
				} else {
					if (whiteCount == 4) {
						white = -SUFFICIENTLY_LARGE_NUMBER;
					} else {
						white += seqScore;
					}
				}
			}
			if (black == 0) {
				result[threatSpace.x][threatSpace.y] = white;
			} else if (white == 0) {
				result[threatSpace.x][threatSpace.y] = black;
			} else {
				if (turn == -1) {
					// white's turn
					result[threatSpace.x][threatSpace.y] = -white + (int)(DEFENSE_WEIGHT * black);
				} else {
					result[threatSpace.x][threatSpace.y] = -black + (int)(DEFENSE_WEIGHT * white);
				}
			}
		}
		return result;
	}

	public void test() {
		System.out.println("<--------test-------->");
		System.out.println("threatSpaces: "+ threatSpaces.toString());
		System.out.println("lookup: "+lookup.toString());
		System.out.println("number of threat spaces: "+lookup.keySet().size());
	}

	public int[][] getScores() {
		return scores;
	}

	// return the best move
	public Point winningMove() {
		nodes = 0;
		time[0] = System.nanoTime();
		Point result = new Point();
		int best;
		List<Point> toVisit = new ArrayList<>(BRANCH_LIMIT);
		MyPQ pq = new MyPQ(BRANCH_LIMIT);
		if (threatSpaces.size() != 1) {
			for (int i=0; i<19; i++) {
				for (int j=0; j<19; j++) {
					if (scores[i][j] != 0) {
						pq.push(scores[i][j], new Point(i, j));
					}
				}
			}
			int largest = pq.peek();
			toVisit.add(pq.pop());
			while (toVisit.size() < BRANCH_LIMIT && pq.peek() > Math.floor(THRESHOLD * largest)) {
				toVisit.add(pq.pop());
			}
		} else {
			// check 2 branches, 8 total
			Point first = new Point();
			for (Point p : threatSpaces.keySet()) {
				first = p;
			}
			if (first.x <= 9) {
				// left side of the board
				if (first.y <= 9) {
					// quadrant 2
					for (int i=0; i<2; i++) {
						// adding diagonals
						toVisit.add(threatSpaces.get(first).get(1).get(i).getP());
					}
					if (first.x > first.y) {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(2).get(i).getP());
						}
					} else {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(0).get(i).getP());
						}
					}
				} else {
					// quadrant 3
					for (int i=0; i<2; i++) {
						toVisit.add(threatSpaces.get(first).get(7).get(i).getP());
					}
					if (first.x > 19 - first.y) {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(6).get(i).getP());
						}
					} else {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(0).get(i).getP());
						}
					}
				}
			} else {
				// right side of the board
				if (first.y <= 9) {
					// quadrant 1
					for (int i=0; i<2; i++) {
						toVisit.add(threatSpaces.get(first).get(3).get(i).getP());
					}
					if (19 - first.x > first.y) {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(2).get(i).getP());
						}
					} else {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(4).get(i).getP());
						}
					}
				} else {
					// quadrant 4
					for (int i=0; i<2; i++) {
						toVisit.add(threatSpaces.get(first).get(5).get(i).getP());
					}
					if (first.x > first.y) {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(4).get(i).getP());
						}
					} else {
						for (int i=0; i<2; i++) {
							toVisit.add(threatSpaces.get(first).get(6).get(i).getP());
						}
					}
				}
			}
		}
		if (turn == 1) {
			best = Integer.MIN_VALUE;
			for (Point p : toVisit) {
				nodes++;
				System.out.println("Searched "+nodes+" nodes");
				int[][] newBoard = addBoard(board, p.x, p.y, turn);
				int newTurn = -turn;
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
				int[][] nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn);
				int val = alphaBeta(newBoard, nextThreats, Integer.MIN_VALUE, Integer.MAX_VALUE, nextLookup,
						nextScores, 0, newTurn);
				if (val >= best) {
					best = val;
					result = p;
				}
			}
		} else {
			best = Integer.MAX_VALUE;
			for (Point p : toVisit) {
				nodes++;
				System.out.println("Searched "+nodes+" nodes");
				int[][] newBoard = addBoard(board, p.x, p.y, turn);
				int newTurn = -turn;
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
				int[][] nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn);
				int val = alphaBeta(newBoard, nextThreats, Integer.MIN_VALUE, Integer.MAX_VALUE, nextLookup,
						nextScores, 0, newTurn);
				if (val <= best) {
					best = val;
					result = p;
				}
			}
		}
		return result;
	}

	// the minimax depth-first search with alphabeta pruning
	// "node" is the combination of board, threats, lookup, scores, and turn
	private int alphaBeta(int[][] board, Map<Point, List<List<PI>>> threatSpaces, int alpha, int beta,
						  Map<Point, List<List<Point>>> lookup, int[][] scores, int depth, int turn) {
		nodes++;
		time[1] = System.nanoTime();
		System.out.println("Reached depth "+depth+" in "+(time[1]-time[0])/1000000+" ms with "+nodes+" nodes");
		if (depth == DEPTH_LIMIT) { // add functionality so that if game over, returns early
			// max node - evaluate and return score
			int total = 0;
			for (int i=0; i<19; i++) {
				for (int j=0; j<19; j++) {
					total += scores[i][j];
				}
			}
			System.out.println("Returning "+total);
			return total;
		}
		List<Point> toVisit = new ArrayList<>(BRANCH_LIMIT);
		MyPQ pq = new MyPQ(BRANCH_LIMIT);
		for (int i=0; i<19; i++) {
			for (int j=0; j<19; j++) {
				if (scores[i][j] != 0) {
					if (Math.abs(scores[i][j]) < WIN_NUMBER) {
						pq.push(scores[i][j], new Point(i, j));
					} else {
						// TODO: consider cases in which overlaps might push a number past 256
						if (scores[i][j] > 0) {
							return SUFFICIENTLY_LARGE_NUMBER;
						} else {
							return -SUFFICIENTLY_LARGE_NUMBER;
						}
					}
				}
			}
		}
		int largest = pq.peek();
		toVisit.add(pq.pop());
		while (toVisit.size() < BRANCH_LIMIT && pq.peek() > Math.floor(THRESHOLD * largest)) {
			toVisit.add(pq.pop());
		}
		if (turn == 1) {
			int val = Integer.MIN_VALUE;
			// maximizing player - should prefer the totals that have higher positive value
			// visit all the places in order and do alpha beta pruning
			for (Point p : toVisit) {
				int[][] newBoard = addBoard(board, p.x, p.y, turn);
				int newTurn = -turn;
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
				int[][] nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn);
				val = Math.max(val, alphaBeta(newBoard, nextThreats, alpha, beta, nextLookup, nextScores, depth + 1, newTurn));
				alpha = Math.max(alpha, val);
				if (alpha >= beta) break;
			}
			return val;
		} else {
			int val = Integer.MAX_VALUE;
			// minimizing player
			for (Point p : toVisit) {
				int[][] newBoard = addBoard(board, p.x, p.y, turn);
				int newTurn = -turn;
				Map<Point, List<List<PI>>> nextThreats = step(p.x, p.y, threatSpaces, lookup, newTurn, newBoard);
				Map<Point, List<List<Point>>> nextLookup = hash(lookup, p.x, p.y, newBoard, newTurn);
				int[][] nextScores = calculateScores(nextLookup, nextThreats, newBoard, newTurn);
				val = Math.min(val, alphaBeta(newBoard, nextThreats, alpha, beta, nextLookup, nextScores, depth + 1, newTurn));
				beta = Math.min(beta, val);
				if (alpha >= beta) break;
			}
			return val;
		}
	}

	private int[][] addBoard(int[][] board, int x, int y, int turn) {
		int[][] result = new int[19][19];
		for (int i=0; i<19; i++) {
			for (int j=0; j<19; j++) {
				result[i][j] = board[i][j];
			}
		}
		result[x][y] = turn;
		return result;
	}

	private List<PI> copyList(List<PI> toCopy) {
		List<PI> result = new ArrayList<>();
		for (int i=0; i<toCopy.size(); i++) {
			result.add(i, new PI(new Point(toCopy.get(i).getP().x, toCopy.get(i).getP().y), toCopy.get(i).getI()));
		}
		return result;
	}

	private int length(List<Point> threatSequence) {
		Point start = threatSequence.get(0), end = threatSequence.get(threatSequence.size() - 1);
		int len2 = (start.x - end.x) * (start.x - end.x) + (start.y - end.y) * (start.y - end.y);
		int len1 = (int)Math.sqrt(len2);
		if (len2 == len1 * len1) {
			return len1;
		} else {
			return (int)Math.sqrt(len2 / 2);
		}
	}
}