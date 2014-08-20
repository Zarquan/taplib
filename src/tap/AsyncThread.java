package tap;

/*
 * This file is part of TAPLibrary.
 * 
 * TAPLibrary is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * TAPLibrary is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with TAPLibrary.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Copyright 2012 - UDS/Centre de Données astronomiques de Strasbourg (CDS)
 */

import uws.UWSException;
import uws.job.JobThread;
import adql.parser.ParseException;
import adql.translator.TranslationException;

public class AsyncThread extends JobThread {

	protected final ADQLExecutor executor;

	public AsyncThread(TAPJob j, ADQLExecutor executor) throws UWSException{
		super(j, "Execute the ADQL query of the TAP request " + j.getJobId());
		this.executor = executor;
	}

	@Override
	protected void jobWork() throws UWSException, InterruptedException{
		try{
			executor.start(this);
		}catch(InterruptedException ie){
			throw ie;
		}catch(UWSException ue){
			throw ue;
		}catch(TAPException te){
			throw new UWSException(te.getHttpErrorCode(), te, te.getMessage());
		}catch(ParseException pe){
			throw new UWSException(UWSException.BAD_REQUEST, pe, pe.getMessage());
		}catch(TranslationException te){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, te, te.getMessage());
		}catch(Exception ex){
			throw new UWSException(UWSException.INTERNAL_SERVER_ERROR, ex, "Error while processing the ADQL query of the job " + job.getJobId() + " !");
		}finally{
			getTAPJob().setExecReport(executor.getExecReport());
		}
	}

	public final TAPJob getTAPJob(){
		return (TAPJob)job;
	}

}
