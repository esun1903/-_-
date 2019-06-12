import java.awt.*;
import java.util.ArrayList;
import java.util.List;

//a. Java의 기본 우선 순위 큐가 실제로 이진 힙이기 때문에 사용자 지정 우선 순위 큐
//b. 상위 5위만 신경 쓰기 때문에 해야 할 계산량을 제한하고 싶다.
//또한 절대값으로 정렬됨
public class MyPQ {
	private int limit, size;
	private List<Integer> top;
	private List<Point> points;

	public MyPQ(int limit) {
		this.limit = limit;
		top = new ArrayList<>(limit);
		points = new ArrayList<>(limit);
		size = 0;
	}

	public void push(int n, Point p) {  
		if (size == 0) {
			top.add(n);
			points.add(p);
			size++;
		} else {
			int place = size;
			for (int i = size - 1; i >= 0; i--) {
				if (Math.abs(top.get(i)) > Math.abs(n)) break;
				place = i;
			}
			if (place < size) {
				if (size < limit) {
					top.add(place, n);
					points.add(place, p);
					size++;
				} else {
					top.remove(size - 1);
					points.remove(size - 1);
					top.add(place, n);
					points.add(place, p);
				}
			}
		}
	}

	public Point pop() {
		top.remove(0);
		size--;
		return points.remove(0);
	}

	public int peek() {
		return top.get(0);
	}
}