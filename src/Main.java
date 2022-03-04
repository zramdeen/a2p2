/*=============================================================================
| Assignment: Problem 2: Viewing the Vase.
|
| Author: Zahid Ramdeen
| Language: Java
|
| To Compile: (from terminal)
| javac Main.java
|
| To Execute: (from terminal) Note: needs at least 2 threads.
| java Main <number of threads>
|
| Class: COP4520 - Concepts of Parallel and Distributed Processing - Spring 2022
| Instructor: Damian Dechev
| Due Date: 3/4/2022
|
+=============================================================================*/

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class Main {
	public static void main(String[] args) throws Exception {
		// obtain command line argument from user.
		if(args.length == 0) {
			System.out.println("enter the number to stop at as an argument (eg: java A1 100)");
			System.exit(0);
		}

		// ensure the value entered is an integer and is a valid positive number
		final int TOTAL_THREADS = Integer.parseInt(args[0]);
		if(TOTAL_THREADS < 2)
			throw new Exception("Needs at least 2 threads");

		MCSLock lock = new MCSLock();

		int N = TOTAL_THREADS;
		Thread tarr[] = new Thread[N];

		// setup threads
		for (int i = 0; i < N; i++) {
			tarr[i] = new Thread(new Guest(lock), "t"+i);
		}

		// start the threads
		for (Thread t:tarr) {
			t.start();
		}
	}
}

/**
 * MCSLock implementation from the book verbatim.
 * The only addition is the Thread.yield() method.
 * This method allows threads with actual work to progress.
 */
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

/**
 * A Guest can visit the Room and view the Vase.
 * Each guest enters the queue and waits to enter the Room.
 * A guest can roll a dice when they are in the Room.
 * If the dice roll is greater than some threshold, they enter the queue again.
 * Once all Guests have exited the Queue, the program exits.
 */
class Guest implements Runnable {
	private boolean again = true;
	private int visits = 0;
	private final MCSLock lock;

	Guest(MCSLock lock){
		this.lock = lock;
	}

	@Override
	public void run() {
		Random r = new Random();
		while(again){
			lock.lock();

			// visit the vase and decide to get back in line
			visits++;
			double roll = r.nextDouble();
			if(Double.compare(roll, 0.2) < 0){ // roll a dice
				again = false;
				System.out.println(Thread.currentThread().getName() + " nice vase. visits = " + visits);
			}

			lock.unlock();
		}
	}
}

