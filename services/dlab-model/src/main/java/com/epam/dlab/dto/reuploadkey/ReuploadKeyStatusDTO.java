package com.epam.dlab.dto.reuploadkey;

import com.epam.dlab.dto.StatusBaseDTO;
import com.google.common.base.MoreObjects;
import lombok.Getter;

@Getter
public class ReuploadKeyStatusDTO extends StatusBaseDTO<ReuploadKeyStatusDTO> {

	private ReuploadKeyCallbackDTO reuploadKeyCallbackDTO;
	private ReuploadKeyStatus reuploadKeyStatus;


	public ReuploadKeyStatusDTO withReuploadKeyCallbackDto(ReuploadKeyCallbackDTO reuploadKeyCallbackDTO) {
		this.reuploadKeyCallbackDTO = reuploadKeyCallbackDTO;
		return this;
	}

	public ReuploadKeyStatusDTO withReuploadKeyStatus(ReuploadKeyStatus status) {
		this.reuploadKeyStatus = status;
		return this;
	}

	@Override
	public MoreObjects.ToStringHelper toStringHelper(Object self) {
		return super.toStringHelper(self)
				.add("reuploadKeyStatus", reuploadKeyStatus)
				.add("reuploadKeyCallbackDTO", reuploadKeyCallbackDTO);
	}
}
