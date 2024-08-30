package se.liu.ida.hefquin.engine.federation.access;

import java.util.List;

import se.liu.ida.hefquin.engine.wrappers.lpg.data.TableRecord;

public interface RecordsResponse extends DataRetrievalResponse {
    List<TableRecord> getResponse();
}
