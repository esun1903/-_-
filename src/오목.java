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

public class ���� extends JFrame {
    private static final int offset = 50;  //�����ǰ�� ���� -> ��ư�� �������� ����
    private static final int square = 40;  //�������� ũ��
    private static final int pieceSize = 15; //���� ũ�� 
    private static final int fontSize = 20; //������z�ȿ� �ִ� �۾�üũ��
    private static final String filePath = "images/background.PNG ";  //������ url
	//private static final String serverIP = "138.197.80.169";
	private static final boolean TEST = false;   //�׽�Ʈ false�� �ϸ� �ٵ��� ����
    private Point click3, created;
    private List<Point> pieces;  //���콺 Ŭ���� ��ġ���� ��� list? ���� 
    private List<Set<Point>> set34; 
    private int mouseX, mouseY, show; //mouseX: ���콺 �����϶� x�� ��ǥ , mouseY:���콺 �����϶� y�� ��ǥ
    private int bUndo = 0, wUndo = 0, startState = 1;    //startState�� �޴��� � ������ �����ִ� ���� 
    private String font = "Lucina Grande"; //font �����ִ� 
    //ifWon : �̰���� ���̰���� üũ���ִ� ����, showNum : ���� ���� ���� ���ڸ� �����ִ� ����, 
    private boolean ifWon = false, showNum = false, calculating = false, AIMode = false,  
      			online = false, connecting = false;
    
    private boolean BakAIMode = false;
   
    
    private BufferedImage image;
	private Jack AI;
	private ClientCommunicator comm;
	
	
	//�����ڰ� �� �̰� �߰��ϸ� ���� �� ���ƿ�!//
	// TODO: ���ӿ� ������ ��ġ�� �ʴ� ������� ������ ������ �� ���� ��� ���ܸ� ó��
    // TODO: Ÿ�̸� ��Ӵٿ�, ���� ���� ����, ������ �Ϸ�Ǹ� �ڵ� ����
	// TODO: Javadoc ��Ÿ�Ϸ� ������Ʈ, �κ������� �Ϸ�� ������ ����� ��ȣ �ۿ� �ε� ����
	// TODO: ������ �ε� ������ �˸��� ���� ȭ���� Ƣ���
	// TODO: �Ҹ��ֱ�

    // ������
    public ����() {
        super("����");
        // load in background here and not at paintComponent to greatly boost FPS
		if (!TEST) {
			try {
				image = ImageIO.read(getClass().getClassLoader().getResourceAsStream(filePath));
			} catch (IOException e) {
				System.err.println("�̹����� "+filePath+"' �� �������� �ʽ��ϴ�");
				System.exit(-1);
			}
		}
        // ĵ���� �� GUI(��ư ��) �ۼ��� �����ִ� component
        JComponent canvas = setupCanvas();
        JComponent gui = setupGUI();
		
        // ��ư�� ĵ������ �Բ� window�� �־��ִ� �ڵ�
        Container cp = getContentPane();
        cp.setLayout(new BorderLayout(0,10));
        cp.add(canvas, BorderLayout.CENTER);   //�ٵ��� �߾ӿ� ��ġ
        cp.add(gui, BorderLayout.EAST);   //��ư, �� ���� �Ʒ��� ��ġ 
		
        //�Ϲ����� �ʱ�ȭ
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
		this.setVisible(true);
		// ������ �ʱ�ȭ�ϴ�.
        pieces = new ArrayList<>();
		AI = new Jack();
    }

    private JComponent setupCanvas() {
        JComponent canvas = new JComponent() {
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
				// ��� �̹����� â ũ�⿡ �°� ����
				if (!connecting) {
					if (!TEST) {  //�׽�Ʈ���� �ƴҶ� 
						g.drawImage(image,0,0,offset*2+square*18,offset*2+square*17,null); //background�̹��� �簢���� �׸���
						for (int i=0; i<17; i++) { // �⺻ �׸��� �׸��� - ����, ������
							g.setColor(Color.black);
							g.drawLine(offset, offset+i*square, offset+16*square, offset+i*square);
							//drawLine�� �̹����� (offset+i*square, offset) ���� ������ ���� offset+i*square, ���� offset+16*square�� �Ѵ�. 
							g.drawLine(offset+i*square, offset, offset+i*square, offset+16*square);
						       
						}
						
								// dot ũ�⸦ 8���� ����
								g.fillOval(offset+square*(8), offset+square*(8), 6, 6);
							
	
					}
					drawPieces(g);
					drawOverlay(g);
				} 
            }
        };
        canvas.setPreferredSize(new Dimension(offset*2+square*16, offset*2+square*16));   //ȭ�� ������ 
        canvas.addMouseListener(new MouseAdapter() {   //���콺�� �ٵ��� ������ play��
            public void mousePressed(MouseEvent e) {
                play(e.getPoint());
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {  //���콺�� �����̸� �� ����Ʈ�� ��� �׷��ִ� �������Լ�
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getPoint().x;  //���콺 �����϶� x�� ��ǥ
                mouseY = e.getPoint().y;  //���콺 �����϶� y�� ��ǥ
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
		String[] states = { "�� - AI, �� - ���", "�� - ���, �� - AI"};
		JComboBox<String> stateB = new JComboBox<>(states);
		stateB.setBackground(Color.white);
		stateB.addActionListener(e -> {
			if (((JComboBox<String>)e.getSource()).getSelectedItem() == "�� - AI, �� - ���") {
				startState = 1;
				AI.setDefense(0.3);
			} else if (((JComboBox<String>)e.getSource()).getSelectedItem() == "�� - ���, �� - AI"){
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
		//������ �ȵ� 
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
            if (i%2 == 0) { //�浹
                g.setColor(Color.black);  //�÷�����
                g.fillOval(offset+square*pieces.get(i).x-pieceSize, offset+square*pieces.get(i).y-pieceSize,
						pieceSize*2, pieceSize*2);  //�浹 ������ ����
                if (showNum) {
                    g.setColor(Color.white);   //���ں��̰� �ϸ� �浹�ϰ�� -> �ؽ�Ʈ�÷��� ���
                }
            } else {  //�鵹
                g.setColor(Color.white);
                g.fillOval(offset+square*pieces.get(i).x-pieceSize, offset+square*pieces.get(i).y-pieceSize,
						pieceSize*2, pieceSize*2);   //�鵹 ������ ����
                if (showNum) {
                    g.setColor(Color.black);   //���ں��̰� �ϸ� �鵹�ϰ�� -> �ؽ�Ʈ�÷��� ������
                }
            }
            
         // showNum : true�Ǹ� ���ڸ� �׷��ֱ�
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
				         //�� Math�Լ��� round()�Լ��� �Ǽ��� �Ҽ��� ù��° �ڸ��� �ݿø��Ͽ� ������ ����
				int px = Math.round((mouseX-offset+square/2)/square);  //px = (���� ���콺 x��ǥ-����+ ��������ũ�� /2)/�������� ũ��  
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
            int px = Math.round((p.x-offset+square/2)/square);    //���콺�� Ŭ���Ҽ��� �� ��ġ�� �����ִ� ����  
            int py = Math.round((p.y-offset+square/2)/square);
            Point pt = new Point(px, py); //Ŭ���� ��ġ
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
								JOptionPane.showMessageDialog(����.this, "�ڹ��� �¸��Ͽ����ϴ�!!!��",
										"���� ����", JOptionPane.INFORMATION_MESSAGE);
							} else {
								JOptionPane.showMessageDialog(����.this, "������ �¸��Ͽ����ϴ�!!!��",
										"���� ����", JOptionPane.INFORMATION_MESSAGE);
							}
							repaint();
						} else {
							repaint();
							if (AIMode) { //AIMode= true�̸� -> ��ǻ�Ϳ� ����ϴ� ���� �� true�� �� 
								calculating = true;   // 
								double startTime = System.nanoTime();  //nanoTime�̶�� �޼ҵ�� ���� �������� ��� �ð��� �����ϴµ� ���
								                                       // �� Ÿ�̸� ���� ����. 
								Point tmp = AI.winningMove(); 
								pieces.add(tmp);
								set34 = open3(pieces);
								AI.addPoint(tmp.x, tmp.y);
								double endTime = System.nanoTime();
								double duration = (endTime - startTime)/1000000;
								System.out.println("It took "+duration+" ms to calculate the best move");
								calculating = false;
								show = pieces.size();  //���� ����� show�� ���� 
								if (won()) {   //won()�� true�� �Ǹ� ifwon boolean ������ true ��ǻ�� �¸��� ��. 
									ifWon = true;
									JOptionPane.showMessageDialog(����.this, "����ǻ�Ͱ� �¸��Ͽ����ϴ�!!!��", "���� ����",
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
					JOptionPane.showMessageDialog(����.this, "���!", "����", JOptionPane.ERROR_MESSAGE);
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
		if (startState == 1) { // ��ǻ�Ͱ� �� 
			BakAIMode = true;
			AIMode = true;
		} else if (startState == 2) { // ��ǻ�Ͱ� �� 
			AIMode = true;
			pieces.add(new Point(8,8));   //���� ���� ���ƾߵǴ°� ��Ģ. �׷��� �ʱⰪ (8,8)�� ����
			show++;  //show�ϳ� �÷���
			AI.addPoint(8,8);  
		} 
        repaint();
    }

  
    
 //------------ �� �Ͽ콺 ���� ai�� ����Ҷ��� ���ǹ� 
    
    // �Ͽ콺 �� : �� ���� ���� �� �ٷ� ���ÿ� ���� ������ �����ϴ�.
    private boolean legalMove(Point p) {
		// �� ��Ģ�� AI�� �ο� �� �������.
		// TODO: AI�� �չ����� �������� ���̴��� Ȯ���ϴ� ����� �˾Ƴ��� �ٽ� �ѽʽÿ�
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
//-----------������� �Ͽ콺���ε�  �̰� ���� �������� �ϴ� ���ϴ�.
    
    
//-----------------�̰���� Ȯ���Ϸ��� �ϴ� �Լ��� �� ����.     
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

	public void checkWin() {     //�¸��ߴ��� üũ���ִ� �Լ� 
    	set34 = open3(pieces);
		if (won()) {
			ifWon = true;  //won�Լ��� true�̸� ifwon�� Ʈ�� 
			if (pieces.size()%2 == 0) {
				JOptionPane.showMessageDialog(����.this, "�ڹ��� �¸��Ͽ����ϴ�!!!��", "������ ����Ǿ����ϴ�", JOptionPane.INFORMATION_MESSAGE);
			} else {
				JOptionPane.showMessageDialog(����.this, "������ �¸��Ͽ����ϴ�!!!��", "������ ����Ǿ����ϴ�", JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	public void setConnecting(boolean b) {
    	connecting = b;
    	repaint();
	}

    public static void main(String[] cheese) {
        new ����();
    }
    }
