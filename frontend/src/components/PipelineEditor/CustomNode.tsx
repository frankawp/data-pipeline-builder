import React, { memo } from 'react';
import { Handle, Position, NodeProps } from '@xyflow/react';
import { DatabaseOutlined, FilterOutlined, ExportOutlined } from '@ant-design/icons';
import type { PipelineNode } from '../../types';

// 节点颜色配置
const nodeColors = {
  SOURCE: { bg: '#e6f7ff', border: '#1890ff', icon: '#1890ff' },
  TRANSFORMER: { bg: '#fff7e6', border: '#fa8c16', icon: '#fa8c16' },
  TARGET: { bg: '#f6ffed', border: '#52c41a', icon: '#52c41a' },
};

// 节点图标
const nodeIcons = {
  SOURCE: DatabaseOutlined,
  TRANSFORMER: FilterOutlined,
  TARGET: ExportOutlined,
};

interface CustomNodeData extends PipelineNode {
  label: string;
}

const CustomNode: React.FC<NodeProps<CustomNodeData>> = ({ data, selected }) => {
  const colors = nodeColors[data.type];
  const Icon = nodeIcons[data.type];

  return (
    <div
      style={{
        padding: '12px 16px',
        borderRadius: 8,
        backgroundColor: colors.bg,
        border: `2px solid ${selected ? '#1890ff' : colors.border}`,
        minWidth: 150,
        boxShadow: selected ? '0 0 0 2px rgba(24, 144, 255, 0.2)' : 'none',
      }}
    >
      {/* 输入端口 - 非 SOURCE 节点显示 */}
      {data.type !== 'SOURCE' && (
        <Handle
          type="target"
          position={Position.Left}
          style={{
            width: 10,
            height: 10,
            background: colors.border,
            border: '2px solid white',
          }}
        />
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
        <Icon style={{ fontSize: 18, color: colors.icon }} />
        <div>
          <div style={{ fontWeight: 500, fontSize: 14 }}>{data.name}</div>
          <div style={{ fontSize: 12, color: '#8c8c8c' }}>{data.pluginType}</div>
        </div>
      </div>

      {/* 输出端口 - 非 TARGET 节点显示 */}
      {data.type !== 'TARGET' && (
        <Handle
          type="source"
          position={Position.Right}
          style={{
            width: 10,
            height: 10,
            background: colors.border,
            border: '2px solid white',
          }}
        />
      )}
    </div>
  );
};

export default memo(CustomNode);
