package com.olf.jm.logging;

public  class Caller {
	
		private String callerMethodName = "";
		private String callerLineNumber = "";
		private String callerClassName = "";



		public Caller(String preferredMethodName, Throwable t) throws Exception {
			 
			StackTraceElement[] trace = t.getStackTrace();
			boolean setMethodName = false;
			if (trace != null) {

				int traceCount = trace.length, iLoop = 0;
				String className = trace[iLoop].getClassName();

				while (iLoop < traceCount) {

					if (trace[iLoop] != null && !trace[iLoop].getClassName().equalsIgnoreCase( className)) {

						className = trace[iLoop].getClassName();
						break;
					}
					iLoop++;
				}

				if (iLoop < traceCount) {

					StackTraceElement stackTraceElement = trace[iLoop];
					if (stackTraceElement != null) {

						String fileName = stackTraceElement.getFileName();
						if (fileName != null) {
							if (fileName.toLowerCase().endsWith(".java")) {
								fileName = fileName.substring(0, fileName.length() - 5);
							}
						} else {

							fileName = stackTraceElement.getClassName();
							if (fileName != null){
								fileName = fileName.substring(fileName.lastIndexOf(".") + 1);
							}
						}
						int lineNumber = stackTraceElement.getLineNumber();
						String methodName = stackTraceElement.getMethodName();
						if (fileName != null && methodName != null) {

							if (lineNumber >= 0){
								this.callerLineNumber = "" + lineNumber; 
							}
							this.callerMethodName = methodName;
							setMethodName = true;
						} else {
							if (lineNumber >= 0){
								this.callerLineNumber = "" + lineNumber;
							}
							this.callerMethodName = stackTraceElement.toString();
							setMethodName = true;
						}
					}
				}

//				while (iLoop < traceCount) {
//
//					if (trace[iLoop] != null  && trace[iLoop].getMethodName().equalsIgnoreCase(preferredMethodName)) {
//
//						className = trace[iLoop].getClassName();
//						executorList.add(new LoggingBase.Executor(className));
//						iLoop++;
//						break;
//					}
//					iLoop++;
//				}
//
//				while (iLoop < traceCount) {
//
//					if (trace[iLoop] != null && trace[iLoop].getMethodName().equalsIgnoreCase(preferredMethodName)) {
//						executorList.add(new LoggingBase.Executor(trace[iLoop].getClassName()));
//					}
//					iLoop++;
//				}
//				if (executorList.size() == 0){
//					executorList.add(new LoggingBase.Executor(className));
//				}
			}
//			this.executorTrace = (Executor[]) executorList .toArray(new LoggingBase.Executor[0]);
			if(!setMethodName){
				this.callerLineNumber = "0";
				this.callerMethodName =  "Unknown";
			}
		}
		protected String getLineNumber (){
			return this.callerLineNumber;
		}
		protected String getMethodName (){
			return this.callerMethodName;
		}
		protected String getClassName (){
			return this.callerClassName;
		}

		//		protected String getDisplayString(){
//			return callerMethodName + " | " + callerLineNumber + " | ";
//		}

//		private Caller() {
//			throw new Error("Invalid construction");
//		}
		

//		public String toString() {
//			StringBuilder stringBuilder = new StringBuilder(this.getDisplayString());
//			if (this.executorTrace == null || this.executorTrace.length == 0) {
//				stringBuilder.append("executor trace is empty");
//			} else {
//				for (int i = this.executorTrace.length; --i >= 0;) {
//					stringBuilder.append("\n").append(i).append(" ").append((this.executorTrace[i]).classname);
//				}
//			}
//			return stringBuilder.toString();
//		}
	}

