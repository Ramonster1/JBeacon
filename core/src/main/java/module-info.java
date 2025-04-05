module com.jbeacon.core {
	requires org.apache.logging.log4j;
	requires com.lmax.disruptor;

	requires static lombok;

	exports com.jbeacon.core.command;
	exports com.jbeacon.core.exception;
	exports com.jbeacon.core.poll;
}
