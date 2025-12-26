import React from 'react';
import { Card, Collapse, Button, Tooltip } from 'antd';
import {
  DatabaseOutlined,
  FilterOutlined,
  ExportOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { usePipelineStore } from '../../store/pipelineStore';
import type { PipelineNode } from '../../types';

const { Panel } = Collapse;

const NodePanel: React.FC = () => {
  const { connectors, transformers, addNode, nodes } = usePipelineStore();

  const createNode = (
    type: 'SOURCE' | 'TRANSFORMER' | 'TARGET',
    pluginType: string,
    displayName: string
  ) => {
    const newNode: PipelineNode = {
      id: `node-${Date.now()}`,
      name: `${displayName} ${nodes.filter(n => n.pluginType === pluginType).length + 1}`,
      type,
      pluginType,
      config: {},
      position: {
        x: 100 + Math.random() * 200,
        y: 100 + Math.random() * 200,
      },
    };
    addNode(newNode);
  };

  return (
    <Card
      title="节点面板"
      size="small"
      style={{ height: '100%', overflow: 'auto' }}
    >
      <Collapse defaultActiveKey={['source', 'transformer', 'target']} ghost>
        {/* 数据源节点 */}
        <Panel
          header={
            <span>
              <DatabaseOutlined style={{ color: '#1890ff', marginRight: 8 }} />
              数据源
            </span>
          }
          key="source"
        >
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {connectors
              .filter((c) => c.supportsRead)
              .map((connector) => (
                <Tooltip key={connector.type} title={connector.description}>
                  <Button
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={() =>
                      createNode('SOURCE', connector.type, connector.displayName)
                    }
                  >
                    {connector.displayName}
                  </Button>
                </Tooltip>
              ))}
          </div>
        </Panel>

        {/* 转换器节点 */}
        <Panel
          header={
            <span>
              <FilterOutlined style={{ color: '#fa8c16', marginRight: 8 }} />
              转换器
            </span>
          }
          key="transformer"
        >
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {transformers.map((transformer) => (
              <Tooltip key={transformer.type} title={transformer.description}>
                <Button
                  size="small"
                  icon={<PlusOutlined />}
                  onClick={() =>
                    createNode('TRANSFORMER', transformer.type, transformer.displayName)
                  }
                >
                  {transformer.displayName}
                </Button>
              </Tooltip>
            ))}
          </div>
        </Panel>

        {/* 目标节点 */}
        <Panel
          header={
            <span>
              <ExportOutlined style={{ color: '#52c41a', marginRight: 8 }} />
              目标
            </span>
          }
          key="target"
        >
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
            {connectors
              .filter((c) => c.supportsWrite)
              .map((connector) => (
                <Tooltip key={connector.type} title={connector.description}>
                  <Button
                    size="small"
                    icon={<PlusOutlined />}
                    onClick={() =>
                      createNode('TARGET', connector.type, connector.displayName)
                    }
                  >
                    {connector.displayName}
                  </Button>
                </Tooltip>
              ))}
          </div>
        </Panel>
      </Collapse>
    </Card>
  );
};

export default NodePanel;
