import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
	public static void main(String[] args) {
		MCSLock lock = new MCSLock();

		int N = 32;
		Thread tarr[] = new Thread[N];

		// setup threads
		for (int i = 0; i < N; i++) {
			tarr[i] = new Thread(new Worker(lock), "t"+i);
		}

		// start the threads
		for (Thread t:tarr) {
			t.start();
		}
	}
}

class MCSLock {
	AtomicReference<Qnode> tail;
	ThreadLocal<Qnode> myNode;

	public MCSLock(){
		tail = new AtomicReference<Qnode>(null);
		myNode = new ThreadLocal<Qnode>() {
			@Override
			protected Qnode initialValue() {
				return new Qnode();
			}
		};
	}

	class Qnode {
		boolean locked = false;
		Qnode next = null;
	}

	public void lock(){
		Qnode qnode = myNode.get();
		Qnode pred = tail.getAndSet(qnode); // get current tail
		if(pred != null){ // add self to the list
			qnode.locked = true;
			pred.next = qnode;
			while(qnode.locked){ // wait till pred gives up the lock
				Thread.yield(); // important line of code -- allows other threads to work...
			}
		}
	}

	public void unlock(){
		Qnode qnode = myNode.get();
		if(qnode.next == null){
			if(tail.compareAndSet(qnode, null))
				return;
			while(qnode.next == null){ // let successor fill next field
				Thread.yield();
			}
		}
		qnode.next.locked = false; // tell successor the lock is theirs to take
		qnode.next = null; // delete cur node
	}
}

// let workers visit a labyrinth
class Worker implements Runnable {
	private boolean again = true;
	private int visits = 0;
	private MCSLock lock;

	Worker(MCSLock lock){
		this.lock = lock;
	}

	@Override
	public void run() {
		Random r = new Random();
		while(again){
			lock.lock();

			// visit the vase and decide to get back in line
			visits++;
			Double roll = r.nextDouble();
			if(Double.compare(roll, 0.5) < 0){ // roll a dice
				again = false;
				System.out.println(Thread.currentThread().getName() + " nice vase. visits = " + visits);
			}

			lock.unlock();
		}
	}
}

