"""Export a current Ultralytics YOLOv8 checkpoint with Hi3519DV500 RPN custom ops."""

import argparse
import shutil
import types

import torch
from torch.onnx.symbolic_helper import parse_args
from ultralytics import YOLO
from ultralytics.nn.modules.head import Detect
from ultralytics.utils.tal import make_anchors


class DecBBoxFunction(torch.autograd.Function):
    """Represent the vendor DecBBox operator during ONNX export."""

    @staticmethod
    def forward(ctx, input_tensor, bias, num_classes):
        """Return a tracing placeholder with the vendor operator's logical shape."""
        return torch.zeros((1, 1, 6, input_tensor.shape[-1]), dtype=input_tensor.dtype, device=input_tensor.device)

    @staticmethod
    @parse_args("v", "v", "i")
    def symbolic(graph, input_tensor, bias, num_classes):
        """Emit the HiSilicon DecBBox custom operator with the trained class count."""
        return graph.op(
            "custom_ops::DecBBox",
            input_tensor,
            bias,
            num_anchors_i=400,
            num_coords_i=4,
            num_classes_i=num_classes,
            gridH_i=20,
            gridW_i=20,
            imgW_i=640,
            imgH_i=640,
            useClassId_i=1,
            calc_mode_i=8,
            clip_bbox_i=1,
            share_loc_i=1,
            multi_label_i=0,
        )


class FilterFunction(torch.autograd.Function):
    """Represent the vendor score filter operator during ONNX export."""

    @staticmethod
    def forward(ctx, input_tensor):
        """Keep the tracing tensor unchanged because ATC supplies the real operator."""
        return input_tensor

    @staticmethod
    def symbolic(graph, input_tensor):
        """Emit the HiSilicon Filter custom operator."""
        return graph.op("custom_ops::Filter", input_tensor, topK_i=300, low_score_thresh_f=0.9)


class SortFunction(torch.autograd.Function):
    """Represent the vendor result sorting operator during ONNX export."""

    @staticmethod
    def forward(ctx, input_tensor, class_count):
        """Keep the tracing tensor unchanged because ATC supplies the real operator."""
        return input_tensor

    @staticmethod
    @parse_args("v", "i")
    def symbolic(graph, input_tensor, class_count):
        """Emit the HiSilicon Sort custom operator for one merged result stream."""
        return graph.op("custom_ops::Sort", input_tensor, topK_i=300, multi_sort_i=0, class_num_i=1)


class NmsFunction(torch.autograd.Function):
    """Represent the vendor NMS operator during ONNX export."""

    @staticmethod
    def forward(ctx, input_tensor):
        """Keep the tracing tensor unchanged because ATC supplies the real operator."""
        return input_tensor

    @staticmethod
    def symbolic(graph, input_tensor):
        """Emit the HiSilicon NMS custom operator."""
        return graph.op("custom_ops::NMS", input_tensor, topK_i=300, nms_thresh_f=0.5)


def hisilicon_inference(self, features):
    """Decode YOLOv8 heads and append the custom RPN pipeline expected by SVP ACL."""
    shape = features[0].shape
    concatenated = torch.cat([feature.view(shape[0], self.no, -1) for feature in features], 2)
    if self.dynamic or self.shape != shape:
        self.anchors, self.strides = (value.transpose(0, 1) for value in make_anchors(features, self.stride, 0.5))
        self.shape = shape
    box, classes = concatenated.split((self.reg_max * 4, self.nc), 1)
    decoded_boxes = self.decode_bboxes(self.dfl(box), self.anchors.unsqueeze(0), xywh=False) * self.strides
    rpn_input = torch.cat((decoded_boxes, classes), 1).view(1, 1, 4 + self.nc, -1)
    bias = torch.tensor([0.0], dtype=rpn_input.dtype, device=rpn_input.device)
    result = DecBBoxFunction.apply(rpn_input, bias, self.nc)
    result = FilterFunction.apply(result)
    result = SortFunction.apply(result, self.nc)
    return NmsFunction.apply(result)


def parse_arguments():
    """Parse the checkpoint, output path, and fixed image size."""
    parser = argparse.ArgumentParser()
    parser.add_argument("--model", required=True)
    parser.add_argument("--output", required=True)
    parser.add_argument("--imgsz", type=int, default=640)
    return parser.parse_args()


def main():
    """Patch only the loaded Detect head and export a fixed-shape opset-13 ONNX file."""
    args = parse_arguments()
    yolo = YOLO(args.model)
    detect = next(module for module in yolo.model.modules() if isinstance(module, Detect))
    detect._inference = types.MethodType(hisilicon_inference, detect)
    exported_path = yolo.export(
        format="onnx",
        opset=13,
        imgsz=args.imgsz,
        batch=1,
        dynamic=False,
        simplify=False,
    )
    if exported_path != args.output:
        shutil.move(exported_path, args.output)
    print(args.output)


if __name__ == "__main__":
    main()
