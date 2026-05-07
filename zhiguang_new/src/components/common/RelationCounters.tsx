import { useEffect, useState } from "react";
import styles from "./RelationCounters.module.css";

type RelationCountersProps = {
  userId?: number;
};

const RelationCounters = ({ userId }: RelationCountersProps) => {
  //    占位符
  //todo
  return <div className={styles.card}>{userId} 占位符！</div>;
};

export default RelationCounters;
