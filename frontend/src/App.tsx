import React, { useEffect, useState } from 'react';
import { Layout, Menu, Button, Modal, Input, Form, message, List, Card, Tag, Popconfirm } from 'antd';
import {
  PlusOutlined,
  PlayCircleOutlined,
  SaveOutlined,
  FolderOpenOutlined,
  DeleteOutlined,
} from '@ant-design/icons';
import PipelineEditor from './components/PipelineEditor';
import NodePanel from './components/NodePanel';
import ConfigPanel from './components/ConfigPanel';
import { usePipelineStore } from './store/pipelineStore';
import { pipelineApi, connectorApi, transformerApi } from './services/api';
import type { Pipeline } from './types';

const { Header, Sider, Content } = Layout;

const App: React.FC = () => {
  const {
    currentPipeline,
    setCurrentPipeline,
    setConnectors,
    setTransformers,
    getPipelineData,
  } = usePipelineStore();

  const [pipelines, setPipelines] = useState<Pipeline[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isListModalOpen, setIsListModalOpen] = useState(false);
  const [form] = Form.useForm();
  const [executing, setExecuting] = useState(false);

  // 加载连接器和转换器列表
  useEffect(() => {
    const loadPlugins = async () => {
      try {
        const [connectors, transformers] = await Promise.all([
          connectorApi.getAll(),
          transformerApi.getAll(),
        ]);
        setConnectors(connectors);
        setTransformers(transformers);
      } catch (error) {
        console.error('Failed to load plugins:', error);
        message.error('加载插件列表失败');
      }
    };
    loadPlugins();
  }, []);

  // 加载 Pipeline 列表
  const loadPipelines = async () => {
    try {
      const list = await pipelineApi.getAll();
      setPipelines(list);
    } catch (error) {
      console.error('Failed to load pipelines:', error);
    }
  };

  // 创建新 Pipeline
  const handleCreate = async () => {
    try {
      const values = await form.validateFields();
      const newPipeline = await pipelineApi.create({
        name: values.name,
        description: values.description,
        nodes: [],
        edges: [],
      });
      setCurrentPipeline(newPipeline);
      setIsModalOpen(false);
      form.resetFields();
      message.success('Pipeline 创建成功');
    } catch (error) {
      console.error('Failed to create pipeline:', error);
      message.error('创建失败');
    }
  };

  // 保存 Pipeline
  const handleSave = async () => {
    if (!currentPipeline?.id) {
      message.warning('请先创建或打开一个 Pipeline');
      return;
    }

    try {
      const data = getPipelineData();
      await pipelineApi.update(currentPipeline.id, data);
      message.success('保存成功');
    } catch (error) {
      console.error('Failed to save pipeline:', error);
      message.error('保存失败');
    }
  };

  // 打开 Pipeline
  const handleOpen = async (id: string) => {
    try {
      const pipeline = await pipelineApi.getById(id);
      setCurrentPipeline(pipeline);
      setIsListModalOpen(false);
      message.success(`已打开: ${pipeline.name}`);
    } catch (error) {
      console.error('Failed to open pipeline:', error);
      message.error('打开失败');
    }
  };

  // 删除 Pipeline
  const handleDelete = async (id: string) => {
    try {
      await pipelineApi.delete(id);
      if (currentPipeline?.id === id) {
        setCurrentPipeline(null);
      }
      loadPipelines();
      message.success('删除成功');
    } catch (error) {
      console.error('Failed to delete pipeline:', error);
      message.error('删除失败');
    }
  };

  // 执行 Pipeline
  const handleExecute = async () => {
    if (!currentPipeline?.id) {
      message.warning('请先保存 Pipeline');
      return;
    }

    // 先保存
    await handleSave();

    setExecuting(true);
    try {
      const result = await pipelineApi.execute(currentPipeline.id);
      if (result.status === 'COMPLETED') {
        message.success(`执行成功！处理了 ${result.totalRecordsProcessed} 条记录`);
      } else {
        message.error(`执行失败: ${result.errorMessage}`);
      }
    } catch (error: any) {
      console.error('Failed to execute pipeline:', error);
      message.error(`执行失败: ${error.message}`);
    } finally {
      setExecuting(false);
    }
  };

  return (
    <Layout style={{ height: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 24px' }}>
        <div style={{ color: 'white', fontSize: 18, fontWeight: 'bold' }}>
          Pipeline Builder
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <Button icon={<PlusOutlined />} onClick={() => setIsModalOpen(true)}>
            新建
          </Button>
          <Button
            icon={<FolderOpenOutlined />}
            onClick={() => {
              loadPipelines();
              setIsListModalOpen(true);
            }}
          >
            打开
          </Button>
          <Button icon={<SaveOutlined />} onClick={handleSave}>
            保存
          </Button>
          <Button
            type="primary"
            icon={<PlayCircleOutlined />}
            onClick={handleExecute}
            loading={executing}
          >
            执行
          </Button>
        </div>
      </Header>

      <Layout>
        <Sider width={250} theme="light" style={{ borderRight: '1px solid #f0f0f0' }}>
          <NodePanel />
        </Sider>

        <Content style={{ position: 'relative', background: '#fafafa' }}>
          {currentPipeline ? (
            <PipelineEditor />
          ) : (
            <div style={{
              height: '100%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: '#999',
            }}>
              点击"新建"创建一个 Pipeline，或点击"打开"加载已有 Pipeline
            </div>
          )}
        </Content>

        <Sider width={300} theme="light" style={{ borderLeft: '1px solid #f0f0f0' }}>
          <ConfigPanel />
        </Sider>
      </Layout>

      {/* 新建 Pipeline 弹窗 */}
      <Modal
        title="新建 Pipeline"
        open={isModalOpen}
        onOk={handleCreate}
        onCancel={() => setIsModalOpen(false)}
        okText="创建"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item
            name="name"
            label="名称"
            rules={[{ required: true, message: '请输入 Pipeline 名称' }]}
          >
            <Input placeholder="输入 Pipeline 名称" />
          </Form.Item>
          <Form.Item name="description" label="描述">
            <Input.TextArea rows={3} placeholder="可选的描述信息" />
          </Form.Item>
        </Form>
      </Modal>

      {/* Pipeline 列表弹窗 */}
      <Modal
        title="打开 Pipeline"
        open={isListModalOpen}
        onCancel={() => setIsListModalOpen(false)}
        footer={null}
        width={600}
      >
        <List
          dataSource={pipelines}
          renderItem={(item) => (
            <List.Item
              actions={[
                <Button type="link" onClick={() => handleOpen(item.id)}>
                  打开
                </Button>,
                <Popconfirm
                  title="确定删除？"
                  onConfirm={() => handleDelete(item.id)}
                  okText="确定"
                  cancelText="取消"
                >
                  <Button type="link" danger>
                    删除
                  </Button>
                </Popconfirm>,
              ]}
            >
              <List.Item.Meta
                title={item.name}
                description={item.description || '暂无描述'}
              />
              <Tag color={item.status === 'ACTIVE' ? 'green' : 'default'}>
                {item.status}
              </Tag>
            </List.Item>
          )}
          locale={{ emptyText: '暂无 Pipeline' }}
        />
      </Modal>
    </Layout>
  );
};

export default App;
