package com.epam.dlab.dto.reuploadkey;

import com.epam.dlab.dto.StatusBaseDTO;
import com.google.common.base.MoreObjects;
import lombok.Getter;

@Getter
public class ReuploadKeyStatusDTO extends StatusBaseDTO<ReuploadKeyStatusDTO> {

	private ReuploadKeyDTO reuploadKeyDTO;
	private ReuploadKeyStatus reuploadKeyStatus;


	public ReuploadKeyStatusDTO withReuploadKeyDTO(ReuploadKeyDTO reuploadKeyDTO) {
		this.reuploadKeyDTO = reuploadKeyDTO;
		return this;
	}

	public ReuploadKeyStatusDTO withStatus(ReuploadKeyStatus status) {
		this.reuploadKeyStatus = status;
		return withStatus(status.name());
	}

	@Override
	public MoreObjects.ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("reuploadKeyStatus", reuploadKeyStatus)
				.add("reuploadKeyDTO", reuploadKeyDTO);
	}
}
