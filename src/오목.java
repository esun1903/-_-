 import javax.imageio.ImageIO;
import javax.print.DocFlavor.URL;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class 오목 extends JFrame {
    private static final int offset = 50;  //오목판경계 길이 -> 버튼과 오목판의 사이
    private static final int square = 40;  //오목판의 크기
    private static final int pieceSize = 15; //돌의 크기 
    private static final int fontSize = 20; //오목판z안에 있는 글씨체크기
    private static final String filePath = "images/background.PNG ";  //배경사진 url
	//private static final String serverIP = "138.197.80.169";
	private static final boolean TEST = false;   //테스트 false로 하면 바둑판 생김
    private Point click3, created;
    private List<Point> pieces;  //마우스 클릭한 위치들이 담긴 list? 같음 
    private List<Set<Point>> set34; 
    private int mouseX, mouseY, show; //mouseX: 마우스 움직일때 x축 좌표 , mouseY:마우스 움직일때 y축 좌표
    private int bUndo = 0, wUndo = 0, startState = 1;    //startState는 메뉴가 어떤 것인지 말해주는 변수 
    private String font = "Lucina Grande"; //font 정해주는 
    //ifWon : 이겼는지 안이겼는지 체크해주는 변수, showNum : 내가 넣은 돌에 숫자를 보여주는 변수, 
    private boolean ifWon = false, showNum = false, calculating = false, AIMode = false,  
      			online = false, connecting = false;
    
    private boolean BakAIMode = false;
   
    
    private BufferedImage image;
	private Jack AI;
	private ClientCommunicator comm;
	
	
	//개발자가 쓴 이거 추가하면 좋을 것 같아요!//
	// TODO: 게임에 영향을 미치지 않는 방식으로 서버에 연결할 수 없는 경우 예외를 처리
    // TODO: 타이머 드롭다운, 파일 형식 지정, 게임이 완료되면 자동 저장
	// TODO: Javadoc 스타일로 업데이트, 부분적으로 완료된 게임의 잭과의 상호 작용 로딩 실험
	// TODO: 게임이 로딩 중임을 알리기 위해 화면을 튀기다
	// TODO: 소리넣기

    // 생성자
    public 오목() {
        super("오목");
        // load in background here and not at paintComponent to greatly boost FPS
		if (!TEST) {
			try {
				image = ImageIO.read(getClass().getClassLoader().getResourceAsStream(filePath));
			} catch (IOException e) {
				System.err.println("이미지가 "+filePath+"' 에 존재하지 않습니다");
				System.exit(-1);
			}
		}
        // 캔버스 및 GUI(버튼 등) 작성을 도와주는 component
        JComponent canvas = setupCanvas();
        JComponent gui = setupGUI();
		
        // 버튼과 캔버스를 함께 window에 넣어주는 코드
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout(0,10));
        cp.add(canvas, BorderLayout.CENTER);   //바둑판 중앙에 배치
        cp.add(gui, BorderLayout.EAST);   //버튼, 맵 설정 아래에 배치 
		
        //일반적인 초기화
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
		this.setVisible(true);
		// 게임을 초기화하다.
        pieces = new ArrayList<>();
		AI = new Jack();
    }

    private JComponent setupCanvas() {
        JComponent canvas = new JComponent() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
				// 배경 이미지를 창 크기에 맞게 설정
				if (!connecting) {
					if (!TEST) {  //테스트중이 아닐때 
						g.drawImage(image,0,0,offset*2+square*18,offset*2+square*17,null); //background이미지 사각형에 그리기
						for (int i=0; i<17; i++) { // 기본 그리드 그리기 - 수평선, 수직선
							g.setColor(Color.black);
							g.drawLine(offset, offset+i*square, offset+16*square, offset+i*square);
							//drawLine은 이미지를 (offset+i*square, offset) 부터 시작해 넓이 offset+i*square, 높이 offset+16*square로 한다. 
							g.drawLine(offset+i*square, offset, offset+i*square, offset+16*square);
						       
						}
						
								// dot 크기를 8으로 설정
								g.fillOval(offset+square*(8), offset+square*(8), 6, 6);
							
	
					}
					drawPieces(g);
					drawOverlay(g);
				} 
            }
        };
        canvas.setPreferredSize(new Dimension(offset*2+square*16, offset*2+square*16));   //화면 사이즈 
        canvas.addMouseListener(new MouseAdapter() {   //마우스에 바둑을 놓을때 play됨
            public void mousePressed(MouseEvent e) {
                play(e.getPoint());
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {  //마우스가 움직이면 그 포인트를 잡아 그려주는 리스너함수
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getPoint().x;  //마우스 움직일때 x축 좌표
                mouseY = e.getPoint().y;  //마우스 움직일때 y축 좌표
                repaint();
            }
        });
        return canvas;
    } 
    private JComponent setupGUI() {
		
        JButton clear;
        
        if (!TEST) {
        	java.net.URL imgUrl = this.getClass().getResource("images/start_omok.png");
        	ImageIcon defaultIcon = new ImageIcon(imgUrl);
        	clear = new JButton(defaultIcon);
        	clear.setBackground(Color.white);
        	clear.setSize(1,3);
        	clear.setVisible(true);
        } else {
        	clear = new JButton("restart");
        	clear.setSize(30,30);
		}
        
       
        clear.addActionListener(e -> clear());
		String[] states = { "흰 - AI, 검 - 사람", "흰 - 사람, 검 - AI"};
		JComboBox<String> stateB = new JComboBox<>(states);
		stateB.setBackground(Color.white);
		stateB.addActionListener(e -> {
			if (((JComboBox<String>)e.getSource()).getSelectedItem() == "흰 - AI, 검 - 사람") {
				startState = 1;
				AI.setDefense(0.3);
			} else if (((JComboBox<String>)e.getSource()).getSelectedItem() == "흰 - 사람, 검 - AI"){
				startState = 2;
				AI.setDefense(0.92);
			} else {
				startState = 3;
			}
			if (show == 0) clear();
			System.out.println("Start state changed to: "+startState);
		});
		/*
		JButton first = new JButton("<<");
		//적용이 안됨 
		first.setSize(400,400);
		
		first.addActionListener(e -> {
			if (pieces.size() > 0) {
				show = 1;
				repaint();
			}
		});
		*/   
		
		JButton prev;
		java.net.URL prevUrl = this.getClass().getResource("prev_button.png");
		ImageIcon prevIcon = new ImageIcon(prevUrl);
    	prev = new JButton(prevIcon);
    	prev.setBackground(Color.white);
		prev.addActionListener(e -> {
			if (show > 1) {
				show--;
				repaint();
			}
		});
		JButton next;
		java.net.URL nextUrl = this.getClass().getResource("next_button.png");
		ImageIcon nextIcon = new ImageIcon(nextUrl);
    	next = new JButton(nextIcon);
    	next.setBackground(Color.white);
		next.addActionListener(e -> {
			if (show < pieces.size()) {
				show++;
				repaint();
			}
		});
		/* 
		JButton last = new JButton(">>");
		last.addActionListener(e -> {
			show = pieces.size();
			repaint();
		});*/
        JComponent gui = new JPanel();
        JPanel jp = new JPanel();
        
        jp.setLayout(new BorderLayout(10,20));
        
//        stateB.setBounds(10, 10, 10, 10);
//        clear.setBounds(110, 110, 100, 100);
//        prev.setBounds(700, 500, 100, 100);
//        next.setBounds(810, 500, 100, 100);
	  
        
        
        gui.setBackground(Color.white);
        jp.add(stateB, BorderLayout.NORTH);
        jp.add(clear, BorderLayout.SOUTH);
     //   gui.add(first);
        jp.add(prev, BorderLayout.WEST);
        jp.add(next, BorderLayout.EAST);
      //  gui.add(last);
		
        gui.add(jp);
        
        return gui;
    }



    private void drawPieces(Graphics g) {
        FontMetrics metrics = g.getFontMetrics(new Font(font, Font.PLAIN, fontSize));
        FontMetrics metrics2 = g.getFontMetrics(new Font(font, Font.PLAIN, fontSize-4));
        for (int i=0; i<show; i++) {
            if (i%2 == 0) { //흑돌
                g.setColor(Color.black);  //컬러지정
                g.fillOval(offset+square*pieces.get(i).x-pieceSize, offset+square*pieces.get(i).y-pieceSize,
						pieceSize*2, pieceSize*2);  //흑돌 사이즈 지정
                if (showNum) {
                    g.setColor(Color.white);   //숫자보이게 하면 흑돌일경우 -> 텍스트컬러는 흰색
                }
            } else {  //백돌
                g.setColor(Color.white);
                g.fillOval(offset+square*pieces.get(i).x-pieceSize, offset+square*pieces.get(i).y-pieceSize,
						pieceSize*2, pieceSize*2);   //백돌 사이즈 지정
                if (showNum) {
                    g.setColor(Color.black);   //숫자보이게 하면 백돌일경우 -> 텍스트컬러는 검정색
                }
            }
            
         // showNum : true되면 숫자를 그려주기
			if (showNum) { 
				if (i<99) {
					g.setFont(new Font(font, Font.PLAIN, fontSize));
					g.drawString(Integer.toString(i + 1), offset + square * pieces.get(i).x
							- (metrics.stringWidth(Integer.toString(i + 1))) / 2, offset+square*pieces.get(i).y
							- (metrics.getHeight()) / 2 + metrics.getAscent());
				} /*else {
					g.setFont(new Font(font, Font.PLAIN,fontSize - 4)); // 3-digits get decreased font size
					g.drawString(Integer.toString(i + 1), offset + square * pieces.get(i).x
							- (metrics2.stringWidth(Integer.toString(i + 1))) / 2, offset+square*pieces.get(i).y
							- (metrics2.getHeight()) / 2 + metrics2.getAscent());
				}*/
			}
        }
        if (TEST) {
			g.setFont(new Font(font, Font.PLAIN, fontSize - 4));
			int[][] scores = AI.getScores();
        	for (int i=0; i<15; i++) {
        		for (int j=0; j<15; j++) {
        			if (scores[i][j] > 0) {
						g.setColor(Color.blue);
					} else if (scores[i][j] < 0) {
						g.setColor(Color.red);
					} else {
        				g.setColor(Color.gray);
					}
        			g.drawString(Integer.toString(scores[i][j]), offset + square * i
						- (metrics2.stringWidth(Integer.toString(scores[i][j]))) / 2, offset + square * j
						- (metrics2.getHeight()) / 2 + metrics2.getAscent());
				}
			}
		}
    }

    private void drawOverlay(Graphics g) {
		if (!calculating) {
			if (!ifWon) {   
				         //↓ Math함수의 round()함수는 실수의 소수점 첫번째 자리를 반올림하여 정수로 리턴
				int px = Math.round((mouseX-offset+square/2)/square);  //px = (현재 마우스 x좌표-간격+ 오목판의크기 /2)/오목판의 크기  
				int py = Math.round((mouseY-offset+square/2)/square);
				if (created == null) {
					if (click3 != null) {
						if ((click3.x-px)*(click3.x-px)+(click3.y-py)*(click3.y-py) >= 1) {
							click3 = null;
							return;
						}
						g.setColor(new Color(220,83,74));
						g.fillOval(offset+square*px-pieceSize,offset+square*py-pieceSize,
								pieceSize*2,pieceSize*2);
						return;
					}
					for (Point p : pieces) {
						if ((p.x-px)*(p.x-px)+(p.y-py)*(p.y-py) < 1) {
							g.setColor(new Color(220,83,74));
							g.fillOval(offset+square*px-pieceSize,offset+square*py-pieceSize,
									pieceSize*2,pieceSize*2);
							return;
						}
					}
					if (pieces.size()%2 == 0) {
						g.setColor(new Color(0,0,0,127));
						g.fillOval(offset+square*px-pieceSize, offset+square*py-pieceSize,
								pieceSize*2, pieceSize*2);
					} else {
						g.setColor(new Color(255,255,255,127));
						g.fillOval(offset+square*px-pieceSize, offset+square*py-pieceSize,
								pieceSize*2, pieceSize*2);
					}
					return;
				}
				if ((created.x-px)*(created.x-px)+(created.y-py)*(created.y-py) >= 1) {
					created = null;
				}
			}
		}
    }

    private void play(Point p) {
        if (!ifWon) {
            int px = Math.round((p.x-offset+square/2)/square);    //마우스가 클릭할순간 그 위치를 갖고있는 변수  
            int py = Math.round((p.y-offset+square/2)/square);
            Point pt = new Point(px, py); //클릭한 위치
            if (!pieces.contains(pt)) {  //
            	List<Point> piecesCopy = new ArrayList<>(pieces);
            	piecesCopy.add(pt);
                set34 = open3(piecesCopy);
                if (legalMove(pt)) {
                    if (TEST || AIMode) AI.addPoint(px, py);
                    if (online) {
                    //	comm.send("add "+px+" "+py);
					} else {
                    	pieces.add(pt);
					}
					show = pieces.size();
					created = pt;
					if (!online) {
						if (won()) {
							ifWon = true;
							if (pieces.size()%2 == 0) {
								JOptionPane.showMessageDialog(오목.this, "★백이 승리하였습니다!!!★",
										"게임 종료", JOptionPane.INFORMATION_MESSAGE);
							} else {
								JOptionPane.showMessageDialog(오목.this, "★흑이 승리하였습니다!!!★",
										"게임 종료", JOptionPane.INFORMATION_MESSAGE);
							}
							repaint();
						} else {
							repaint();
							if (AIMode) { //AIMode= true이면 -> 컴퓨터와 대결하는 모드는 다 true가 됨 
								calculating = true;   // 
								double startTime = System.nanoTime();  //nanoTime이라는 메소드는 기준 시점에서 경과 시간을 측정하는데 사용
								                                       // 즉 타이머 같은 존재. 
								Point tmp = AI.winningMove(); 
								pieces.add(tmp);
								set34 = open3(pieces);
								AI.addPoint(tmp.x, tmp.y);
								double endTime = System.nanoTime();
								double duration = (endTime - startTime)/1000000;
								System.out.println("It took "+duration+" ms to calculate the best move");
								calculating = false;
								show = pieces.size();  //돌의 사이즈를 show에 넣음 
								if (won()) {   //won()이 true가 되면 ifwon boolean 변수가 true 컴퓨터 승리가 뜸. 
									ifWon = true;
									JOptionPane.showMessageDialog(오목.this, "★컴퓨터가 승리하였습니다!!!★", "게임 종료",
											JOptionPane.INFORMATION_MESSAGE);
								}
								repaint();
							}
						}
					}
                } else {
                    click3 = pt;
                    pieces.remove(click3);
                    repaint();
					JOptionPane.showMessageDialog(오목.this, "삼삼!", "에러", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
	}

   
    private void clear() {
        pieces = new ArrayList<>();
		set34 = new ArrayList<>();
        bUndo = wUndo = 0;
        show = 0;
        connecting = online = ifWon = calculating = false;
        created = null;
        click3 = null;
        AI = new Jack();
		if (startState == 1) { // 컴퓨터가 백 
			BakAIMode = true;
			AIMode = true;
		} else if (startState == 2) { // 컴퓨터가 흑 
			AIMode = true;
			pieces.add(new Point(8,8));   //흑이 먼저 놓아야되는게 규칙. 그래서 초기값 (8,8)로 설정
			show++;  //show하나 플러스
			AI.addPoint(8,8);  
		} 
        repaint();
    }

  
    
 //------------ 이 하우스 룰은 ai와 대결할때는 무의미 
    
    // 하우스 룰 : 세 개의 돌을 두 줄로 동시에 여는 동작을 금지하다.
    private boolean legalMove(Point p) {
		// 그 규칙은 AI와 싸울 때 사라진다.
		// TODO: AI가 합법적인 움직임을 보이는지 확인하는 방법을 알아내면 다시 켜십시오
		for (Set<Point> set : set34) {
			if (set.contains(p) && set.size() == 3) {
				for (Point neighbor : set) {
					for (Set<Point> set2 : set34) {
						if (!set.equals(set2) && set2.contains(neighbor) && set2.size() == 3) {
							return false;
						}
					}
				}
			}
		}
        return true;
    }
//-----------여기까지 하우스룰인데  이거 빼면 오류나서 일단 씁니다.
    
    
//-----------------이겼는지 확인하려고 하는 함수인 것 같다.     
    private boolean won() {   
        if (pieces.size() < 9) {
            return false;
        }
        for (Set<Point> set : set34) {
            if (set.size() == 4) {
                List<Point> points = new ArrayList<>();
                for (Point p : set) {
                    points.add(p);
                }
                if (points.get(0).x == points.get(1).x) { // they are on vertical line
                    points.sort((Point o1, Point o2) -> o1.y - o2.y);
                } else { // either horizontal or diagonal line
                    points.sort((Point o1, Point o2) -> o1.x - o2.x);
                }
                for (int i=(pieces.size()%2+1)%2; i<pieces.size(); i=i+2) {
                    if (pieces.get(i).equals(new Point(2*points.get(0).x-points.get(1).x,2*points.get(0).y
							-points.get(1).y)) || pieces.get(i).equals(new Point(2*points.get(3).x-points.get(2).x,
							2*points.get(3).y-points.get(2).y))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // finds list of all sets of 3 adjacent points without any blockages and 4 that have at least one opening
	// quick and dirty way of finding open sets of 3 and 4, for checking for users' legal move and win conditions
    private List<Set<Point>> open3(List<Point> points) {
        List<Set<Point>> result = new ArrayList<>();
        for (int i=(points.size()%2+1)%2; i<points.size(); i=i+2) {
            Point p1 = points.get(i);
            for (int j=i+2; j<points.size(); j=j+2) {
                Point p2 = points.get(j);
                if ((p1.x-p2.x)*(p1.x-p2.x)+(p1.y-p2.y)*(p1.y-p2.y) <= 2) { // if p1 and p2 are adjacent
                    for (int k=(points.size()%2+1)%2; k<points.size(); k=k+2) {
                        if (k != i && k != j) {
							Point p3 = points.get(k);
							boolean passed = false;
							boolean blocked = false;
							Point p41, p42;
							p41 = p42 = new Point();
							if (p3.x!=0 && p3.x!=18 && p3.y!=0 && p3.y!=18) {
								if (p3.equals(new Point(2*p1.x-p2.x, 2*p1.y-p2.y))) { // p3 p1 p2
									if (p2.x!=0 && p2.x !=18 && p2.y!=0 && p2.y!=18) { // boundary check
										p41 = new Point(2*p2.x-p1.x, 2*p2.y-p1.y); // checking both ends
										p42 = new Point(2*p3.x-p1.x, 2*p3.y-p1.y);
										passed = true;
									}
								} else if (p3.equals(new Point(2*p2.x-p1.x,2*p2.y-p1.y))) { // p3 p2 p1
									if (p1.x!=0 && p1.x!=18 && p1.y!=0 && p1.y!=18) {
										p41 = new Point(2*p1.x-p2.x, 2*p1.y-p2.y);
										p42 = new Point(2*p3.x-p2.x, 2*p3.y-p2.y);
										passed = true;
									}
								}
								if (passed) {
									// if either is of other color, throw it out
									for (int n=points.size()%2; n<points.size(); n=n+2) {
										if (points.get(n).equals(p41) || points.get(n).equals(p42)) {
											passed = false;
											blocked = true;
										}
									}
									if (!blocked) {
										for (int n=(points.size()%2+1)%2; n<points.size(); n=n+2) {
											if (points.get(n).equals(p41) || points.get(n).equals(p42)) {
												Set<Point> halfOpenSet4 = new HashSet<>();
												halfOpenSet4.add(p1);
												halfOpenSet4.add(p2);
												halfOpenSet4.add(p3);
												halfOpenSet4.add(points.get(n));
												if (!result.contains(halfOpenSet4)) {
													result.add(halfOpenSet4);
												}
												passed = false;
											}
										}
									}
								}
							}
							if (passed) {
								Set<Point> openSet3 = new HashSet<>();
								openSet3.add(p1);
								openSet3.add(p2);
								openSet3.add(p3);
								if (!result.contains(openSet3)) {
									result.add(openSet3);
								}
							}
						}
                    }
                }
            }
        }
        return result;
    }

    public List<Point> getPieces () {
    	return pieces;
	}

	public void setShow(int show) {
    	this.show = show;
	}

	public void incrementUndo(int i) {
    	if (i == 1) {
    		wUndo++;
			System.out.println("wUndo: "+wUndo);
		} else {
    		bUndo++;
			System.out.println("bUndo: "+bUndo);
		}
	}

	public void checkWin() {     //승리했는지 체크해주는 함수 
    	set34 = open3(pieces);
		if (won()) {
			ifWon = true;  //won함수가 true이면 ifwon도 트루 
			if (pieces.size()%2 == 0) {
				JOptionPane.showMessageDialog(오목.this, "★백이 승리하였습니다!!!★", "게임이 종료되었습니다", JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(오목.this, "★흑이 승리하였습니다!!!★", "게임이 종료되었습니다", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	public void setConnecting(boolean b) {
    	connecting = b;
    	repaint();
	}

    public static void main(String[] cheese) {
        new 오목();
    }
    }
